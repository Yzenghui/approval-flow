package com.approval.backend.service;

import com.approval.backend.common.constant.EnableStatus;
import com.approval.backend.common.constant.NotificationReadStatus;
import com.approval.backend.common.constant.ProcessInstanceStatus;
import com.approval.backend.common.constant.TaskStatus;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.*;
import com.approval.backend.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知落库幂等的集成测试
 * <p>
 * 单测{@link com.approval.backend.service.impl.NotificationServiceImplTest} 里的 {@code DuplicateKeyException} 是 <b> mock 抛出来的</b>，
 * 而真实链路是「MySQL 报 1062 → Spring 的 SQLExceptionTranslator 把它转换成 DuplicateKeyException」
 * 这段映射只发生在框架内部，mock 表达不了，只有连真库才能验证。
 * </p>
 */
@SpringBootTest
@DisplayName("通知服务（真实唯一键）")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private ProcessNodeMapper processNodeMapper;
    @Autowired
    private ProcessInstanceMapper processInstanceMapper;
    @Autowired
    private TaskMapper taskMapper;

    /** 初始化的 admin 用户，满足 notification.recipient_id 的外键约束 */
    private static final Long RECIPIENT_ID = 1L;

    private Long definitionId;
    private Long nodeId;
    private Long instanceId;
    private Long taskId;

    @BeforeEach
    void setUp() {
        ProcessDefinition definition = new ProcessDefinition();
        definition.setName("通知幂等集成测试-" + System.currentTimeMillis());
        definition.setVersion(1);
        definition.setStatus(EnableStatus.ENABLED);
        definition.setFormSchema("{}");
        processDefinitionMapper.insert(definition);
        definitionId = definition.getId();

        ProcessNode node = new ProcessNode();
        node.setDefinitionId(definitionId);
        node.setNodeName("审批节点");
        node.setNodeOrder(1);
        node.setAssigneeType("USER");
        node.setAssigneeExpr(String.valueOf(RECIPIENT_ID));
        processNodeMapper.insert(node);
        nodeId = node.getId();

        ProcessInstance instance = new ProcessInstance();
        instance.setDefinitionId(definitionId);
        instance.setDefinitionName("通知幂等集成测试");
        instance.setInitiatorId(RECIPIENT_ID);
        instance.setFormData("{}");
        instance.setSubmitCount(1);
        instance.setStatus(ProcessInstanceStatus.APPROVING);
        processInstanceMapper.insert(instance);
        instanceId = instance.getId();

        Task task = new Task();
        task.setInstanceId(instanceId);
        task.setNodeId(nodeId);
        task.setNodeName("审批节点");
        task.setAssigneeId(RECIPIENT_ID);
        task.setSubmitRound(1);
        task.setStatus(TaskStatus.PENDING);
        taskMapper.insert(task);
        taskId = task.getId();
    }

    @AfterEach
    void tearDown() {
        // 逆着外键依赖清理：notification 的 instance / task / recipient 三个外键都是 RESTRICT
        if (instanceId != null) {
            notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getInstanceId, instanceId));
        }
        if (taskId != null) {
            taskMapper.deleteById(taskId);
        }
        if (instanceId != null) {
            processInstanceMapper.deleteById(instanceId);
        }
        if (nodeId != null) {
            processNodeMapper.deleteById(nodeId);
        }
        if (definitionId != null) {
            processDefinitionMapper.deleteById(definitionId);
        }
    }

    @Test
    @DisplayName("重复消息撞真实唯一键 uk_task_type：不抛异常，且只留一条通知")
    void duplicateMessageShouldBeSwallowedByRealUniqueKey() {
        ApprovalEventMessage eventMessage = sampleEvent();

        notificationService.save(eventMessage);
        // 先确认首次真的落库了：否则第二次根本不会冲突
        assertEquals(1L, countByTaskId(), "首次落库应成功，否则本用例验证不到唯一键冲突");

        Notification saved = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTaskId, taskId));
        assertNotNull(saved);
        assertEquals(instanceId, saved.getInstanceId());
        assertEquals(RECIPIENT_ID, saved.getRecipientId());
        assertEquals(ApprovalEventMessage.TYPE_REJECT, saved.getType());
        assertEquals(NotificationReadStatus.UNREAD, saved.getIsRead(), "新通知默认未读");

        // 第二次插入会撞 uk_task_type，由 MySQL 抛 1062、Spring 转换成 DuplicateKeyException，
        // 服务内部必须把它按"已处理"吞掉
        assertDoesNotThrow(() -> notificationService.save(eventMessage),
                "重复消息不上抛，否则消费端会一直重投");

        assertEquals(1L, countByTaskId(), "uk_task_type 应保证同一 (task_id, type) 只有一条");
    }

    @Test
    @DisplayName("同一任务的不同 type 互不冲突，各自落一条")
    void differentTypesShouldCoexist() {
        ApprovalEventMessage reject = sampleEvent();
        ApprovalEventMessage todo = sampleEvent();
        todo.setType(ApprovalEventMessage.TYPE_TODO);
        todo.setContent("您有一条待办");

        notificationService.save(reject);
        notificationService.save(todo);

        assertEquals(2L, countByTaskId(), "uk_task_type 是复合唯一键，不同 type 应各存一条");
    }

    private Long countByTaskId() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTaskId, taskId));
    }

    private ApprovalEventMessage sampleEvent() {
        ApprovalEventMessage eventMessage = new ApprovalEventMessage();
        eventMessage.setInstanceId(instanceId);
        eventMessage.setTaskId(taskId);
        eventMessage.setType(ApprovalEventMessage.TYPE_REJECT);
        eventMessage.setRecipientId(RECIPIENT_ID);
        eventMessage.setContent("您的【通知幂等集成测试】申请已被驳回");
        return eventMessage;
    }
}
