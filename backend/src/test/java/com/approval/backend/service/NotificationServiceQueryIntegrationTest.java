package com.approval.backend.service;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.NotificationResponse;
import com.approval.backend.entity.Notification;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.ProcessInstance;
import com.approval.backend.entity.ProcessNode;
import com.approval.backend.entity.Task;
import com.approval.backend.entity.User;
import com.approval.backend.mapper.NotificationMapper;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.ProcessInstanceMapper;
import com.approval.backend.mapper.ProcessNodeMapper;
import com.approval.backend.mapper.TaskMapper;
import com.approval.backend.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知读侧（列表 / 未读数 / 标记已读）的集成测试（真实 SQL）
 */
@SpringBootTest
@DisplayName("通知服务（真实 SQL 的读侧行为）")
class NotificationServiceQueryIntegrationTest {

    /** 三条通知写入同一个秒级时间戳，模拟"一次提交批量产生待办通知"的场景 */
    private static final LocalDateTime SAME_SECOND = LocalDateTime.of(2026, 9, 12, 10, 0, 0);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private ProcessNodeMapper processNodeMapper;
    @Autowired
    private ProcessInstanceMapper processInstanceMapper;
    @Autowired
    private TaskMapper taskMapper;

    /** 造专用收件人：复用已有用户时，其历史通知会让总数/顺序断言随环境漂移 */
    private Long currentUserId;
    private Long otherUserId;

    private Long definitionId;
    private Long nodeId;
    private Long instanceId;
    private Long taskId1;
    private Long taskId2;
    private Long taskId3;

    @BeforeEach
    void setUp() {
        currentUserId = insertUser("notif-me-");
        otherUserId = insertUser("notif-other-");

        ProcessDefinition definition = new ProcessDefinition();
        definition.setName("通知读侧集成测试-" + System.nanoTime());
        definition.setVersion(1);
        definition.setStatus(1);
        definition.setFormSchema("{}");
        processDefinitionMapper.insert(definition);
        definitionId = definition.getId();

        ProcessNode node = new ProcessNode();
        node.setDefinitionId(definitionId);
        node.setNodeName("审批节点");
        node.setNodeOrder(1);
        node.setAssigneeType("USER");
        node.setAssigneeExpr(String.valueOf(currentUserId));
        processNodeMapper.insert(node);
        nodeId = node.getId();

        ProcessInstance instance = new ProcessInstance();
        instance.setDefinitionId(definitionId);
        instance.setDefinitionName("通知读侧集成测试");
        instance.setInitiatorId(currentUserId);
        instance.setFormData("{}");
        instance.setSubmitCount(1);
        instance.setStatus(1);
        processInstanceMapper.insert(instance);
        instanceId = instance.getId();

        // uk_task_type(task_id, type) 决定了同一任务同一类型只能存一条通知，
        // 需要多条通知就必须造多个任务
        taskId1 = insertTask();
        taskId2 = insertTask();
        taskId3 = insertTask();
    }

    @AfterEach
    void tearDown() {
        // 逆着外键依赖清理：notification 与 task 都以 RESTRICT 引用 user
        if (instanceId != null) {
            notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getInstanceId, instanceId));
            taskMapper.delete(new LambdaQueryWrapper<Task>()
                    .eq(Task::getInstanceId, instanceId));
            processInstanceMapper.deleteById(instanceId);
        }
        if (nodeId != null) {
            processNodeMapper.deleteById(nodeId);
        }
        if (definitionId != null) {
            processDefinitionMapper.deleteById(definitionId);
        }
        if (currentUserId != null) {
            userMapper.deleteById(currentUserId);
        }
        if (otherUserId != null) {
            userMapper.deleteById(otherUserId);
        }
    }

    @Test
    @DisplayName("列表：create_time 相同时按 id 降序兜底，翻页不漏不重")
    void shouldFallBackToIdWhenCreateTimeIsIdentical() {
        Long first = insertNotification(taskId1, currentUserId, 0, SAME_SECOND);
        Long second = insertNotification(taskId2, currentUserId, 0, SAME_SECOND);
        Long third = insertNotification(taskId3, currentUserId, 0, SAME_SECOND);

        PageResult<NotificationResponse> page1 =
                notificationService.getNotifications(1, 2, null, null, currentUserId);
        PageResult<NotificationResponse> page2 =
                notificationService.getNotifications(2, 2, null, null, currentUserId);

        // create_time 完全相同时数据库不给任何顺序保证，只有 id DESC 能定序
        assertEquals(List.of(third, second), idsOf(page1), "同秒通知必须按 id 降序，否则翻页结果不可预期");
        assertEquals(List.of(first), idsOf(page2));
    }

    @Test
    @DisplayName("列表：onlyUnread 只返回未读，标记已读后即被排除")
    void shouldReturnUnreadOnly() {
        Long unread = insertNotification(taskId1, currentUserId, 0, SAME_SECOND);
        Long read = insertNotification(taskId2, currentUserId, 0, SAME_SECOND);
        notificationService.markAsRead(read, currentUserId);

        PageResult<NotificationResponse> result =
                notificationService.getNotifications(1, 10, null, true, currentUserId);

        assertEquals(List.of(unread), idsOf(result));

        // 不传 onlyUnread 时两条都应返回，确认筛选是参数驱动的
        assertEquals(2, notificationService
                .getNotifications(1, 10, null, null, currentUserId)
                .getRecords().size());
    }

    @Test
    @DisplayName("列表：按 type 筛选，只返回指定类型")
    void shouldFilterByType() {
        insertNotification(taskId1, currentUserId, 0, SAME_SECOND);
        Long todo = insertNotification(taskId2, currentUserId, 1, SAME_SECOND);

        PageResult<NotificationResponse> result =
                notificationService.getNotifications(1, 10, 1, null, currentUserId);

        assertEquals(List.of(todo), idsOf(result));
        assertEquals(1, result.getRecords().get(0).getType());
    }

    @Test
    @DisplayName("列表：收件人隔离，查不到别人的通知")
    void shouldNotReturnOthersNotifications() {
        Long mine = insertNotification(taskId1, currentUserId, 0, SAME_SECOND);
        insertNotification(taskId2, otherUserId, 0, SAME_SECOND);

        PageResult<NotificationResponse> result =
                notificationService.getNotifications(1, 10, null, null, currentUserId);

        assertEquals(List.of(mine), idsOf(result));
        assertEquals(1L, result.getTotal(), "总数也必须按收件人过滤");
    }

    @Test
    @DisplayName("未读数：只统计自己的通知")
    void shouldCountUnreadPerRecipient() {
        insertNotification(taskId1, currentUserId, 0, SAME_SECOND);
        insertNotification(taskId2, currentUserId, 1, SAME_SECOND);
        insertNotification(taskId3, otherUserId, 0, SAME_SECOND);

        assertEquals(2L, notificationService.getUnreadCount(currentUserId));
        assertEquals(1L, notificationService.getUnreadCount(otherUserId));
    }

    @Test
    @DisplayName("标记已读：越权请求抛业务异常，且数据库里不留任何痕迹")
    void shouldNotTouchOthersNotification() {
        Long others = insertNotification(taskId1, otherUserId, 0, SAME_SECOND);

        assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(others, currentUserId));

        assertEquals(0, notificationMapper.selectById(others).getIsRead(), "越权请求改动了他人数据");
        assertEquals(1L, notificationService.getUnreadCount(otherUserId));
    }

    @Test
    @DisplayName("标记已读：不存在则抛业务异常")
    void shouldThrowWhenNotificationMissing() {
        assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(999_999_999L, currentUserId));
    }

    @Test
    @DisplayName("标记已读：重复标记不报错，状态保持已读且未读数归零")
    void shouldBeIdempotentOnRepeatedMark() {
        Long id = insertNotification(taskId1, currentUserId, 0, SAME_SECOND);

        notificationService.markAsRead(id, currentUserId);
        assertDoesNotThrow(() -> notificationService.markAsRead(id, currentUserId));

        assertEquals(1, notificationMapper.selectById(id).getIsRead());
        assertEquals(0L, notificationService.getUnreadCount(currentUserId));
    }

    @Test
    @DisplayName("全部已读：只清空自己的未读，不影响他人")
    void shouldMarkAllAsReadForCurrentUserOnly() {
        insertNotification(taskId1, currentUserId, 0, SAME_SECOND);
        insertNotification(taskId2, currentUserId, 1, SAME_SECOND);
        insertNotification(taskId3, otherUserId, 0, SAME_SECOND);

        notificationService.markAllAsRead(currentUserId);

        assertEquals(0L, notificationService.getUnreadCount(currentUserId));
        assertEquals(1L, notificationService.getUnreadCount(otherUserId),
                "全部已读不能把别人的通知也标记成已读");
    }

    private List<Long> idsOf(PageResult<NotificationResponse> pageResult) {
        return pageResult.getRecords().stream()
                .map(NotificationResponse::getId)
                .toList();
    }

    private Long insertNotification(Long taskId, Long recipientId, int type, LocalDateTime createTime) {
        Notification notification = new Notification();
        notification.setInstanceId(instanceId);
        notification.setTaskId(taskId);
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setContent("通知读侧集成测试-taskId=" + taskId);
        notification.setIsRead(0);
        notification.setCreateTime(createTime);
        notificationMapper.insert(notification);
        return notification.getId();
    }

    private Long insertTask() {
        Task task = new Task();
        task.setInstanceId(instanceId);
        task.setNodeId(nodeId);
        task.setNodeName("审批节点");
        task.setAssigneeId(currentUserId);
        task.setSubmitRound(1);
        task.setStatus(0);
        taskMapper.insert(task);
        return task.getId();
    }

    private Long insertUser(String usernamePrefix) {
        String unique = usernamePrefix + System.nanoTime();
        User user = new User();
        user.setUsername(unique);
        user.setPassword("test-only");
        user.setName(unique);
        user.setEmail(unique + "@test.local");
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }
}
