package com.approval.backend.service.impl;

import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.Notification;
import com.approval.backend.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通知服务单元测试（纯单测，不启动 Spring）
 */
@DisplayName("通知服务")
class NotificationServiceImplTest {

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
        assertEquals(2, saved.getType());
        assertEquals("审批通过", saved.getContent());
        assertEquals(0, saved.getIsRead(), "新通知默认未读");
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

    private ApprovalEventMessage sampleEvent() {
        ApprovalEventMessage eventMessage = new ApprovalEventMessage();
        eventMessage.setInstanceId(10L);
        eventMessage.setTaskId(1L);
        eventMessage.setType(2);
        eventMessage.setRecipientId(1L);
        eventMessage.setContent("审批通过");
        return eventMessage;
    }
}
