package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.constant.NotificationReadStatus;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.dto.response.NotificationResponse;
import com.approval.backend.entity.Notification;
import com.approval.backend.mapper.NotificationMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通知服务单元测试（不启动 Spring）
 */
@DisplayName("通知服务")
class NotificationServiceImplTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private NotificationMapper notificationMapper;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        notificationService = new NotificationServiceImpl(notificationMapper);
    }

    @Test
    @DisplayName("落库：消息字段完整映射到通知表，且默认未读")
    void shouldMapFieldsAndMarkUnread() {
        notificationService.save(sampleEvent());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(10L, saved.getInstanceId());
        assertEquals(1L, saved.getTaskId());
        assertEquals(1L, saved.getRecipientId());
        assertEquals(ApprovalEventMessage.TYPE_APPROVED, saved.getType());
        assertEquals("审批通过", saved.getContent());
        assertEquals(NotificationReadStatus.UNREAD, saved.getIsRead(), "新通知默认未读");
    }

    @Test
    @DisplayName("幂等：撞唯一键 uk_task_type 时按已处理吞掉，不抛出")
    void shouldSwallowDuplicateKeyException() {
        doThrow(new DuplicateKeyException("uk_task_type"))
                .when(notificationMapper).insert(any(Notification.class));

        assertDoesNotThrow(() -> notificationService.save(sampleEvent()));
    }

    @Test
    @DisplayName("非幂等异常必须抛出，否则消费端会把失败当成功、通知静默丢失")
    void shouldPropagateNonDuplicateException() {
        doThrow(new RuntimeException("db down"))
                .when(notificationMapper).insert(any(Notification.class));

        assertThrows(RuntimeException.class, () -> notificationService.save(sampleEvent()));
    }

    @Test
    @DisplayName("列表：实体字段完整映射到响应，分页总数透传")
    void shouldMapNotificationToResponse() {
        Notification notification = new Notification();
        notification.setId(7L);
        notification.setInstanceId(10L);
        notification.setTaskId(1L);
        notification.setType(ApprovalEventMessage.TYPE_REJECT);
        notification.setContent("您的【请假】申请已被驳回");
        notification.setIsRead(NotificationReadStatus.UNREAD);
        notification.setCreateTime(LocalDateTime.of(2026, 9, 12, 10, 0));
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(List.of(notification));
        page.setTotal(1L);
        doReturn(page).when(notificationMapper).selectPage(any(), any());

        PageResult<NotificationResponse> result =
                notificationService.getNotifications(1, 10, null, null, CURRENT_USER_ID);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());

        NotificationResponse response = result.getRecords().get(0);
        assertEquals(7L, response.getId());
        assertEquals(10L, response.getInstanceId(), "instanceId 缺失前端无法跳转申请详情");
        assertEquals(1L, response.getTaskId(), "taskId 缺失前端无法跳转任务处理");
        assertEquals(ApprovalEventMessage.TYPE_REJECT, response.getType());
        assertEquals("您的【请假】申请已被驳回", response.getContent());
        assertEquals(NotificationReadStatus.UNREAD, response.getIsRead());
        assertEquals(LocalDateTime.of(2026, 9, 12, 10, 0), response.getCreateTime());
    }

    @Test
    @DisplayName("列表：无数据时返回空列表而非 null")
    void shouldReturnEmptyRecordsWhenNoData() {
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0L);
        doReturn(page).when(notificationMapper).selectPage(any(), any());

        PageResult<NotificationResponse> result =
                notificationService.getNotifications(1, 10, null, null, CURRENT_USER_ID);

        assertEquals(0L, result.getTotal());
        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    @DisplayName("未读数：透传 selectCount 结果")
    void shouldReturnUnreadCount() {
        when(notificationMapper.selectCount(any())).thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount(CURRENT_USER_ID));
    }

    @Test
    @DisplayName("标记已读：已是已读时幂等放行，不产生更新")
    void shouldBeIdempotentWhenAlreadyRead() {
        when(notificationMapper.selectById(9L)).thenReturn(notification(9L, CURRENT_USER_ID, 1));

        assertDoesNotThrow(() -> notificationService.markAsRead(9L, CURRENT_USER_ID));
        verify(notificationMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("标记已读：他人通知抛业务异常，不允许改动")
    void shouldRejectOthersNotification() {
        when(notificationMapper.selectById(9L)).thenReturn(notification(9L, OTHER_USER_ID, 0));

        assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(9L, CURRENT_USER_ID));
        verify(notificationMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("标记已读：通知不存在与越权返回同一句错误，不泄露存在性")
    void shouldRejectMissingNotificationWithSameMessage() {
        when(notificationMapper.selectById(9L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(9L, CURRENT_USER_ID));
        assertEquals("通知不存在", exception.getMessage());
        verify(notificationMapper, never()).update(any(), any());
    }

    private Notification notification(Long id, Long recipientId, int isRead) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipientId(recipientId);
        notification.setIsRead(isRead);
        return notification;
    }

    private ApprovalEventMessage sampleEvent() {
        ApprovalEventMessage eventMessage = new ApprovalEventMessage();
        eventMessage.setInstanceId(10L);
        eventMessage.setTaskId(1L);
        eventMessage.setType(ApprovalEventMessage.TYPE_APPROVED);
        eventMessage.setRecipientId(1L);
        eventMessage.setContent("审批通过");
        return eventMessage;
    }
}
