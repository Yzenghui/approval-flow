package com.approval.backend.service.event;

import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.ProcessInstance;
import com.approval.backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 通知事件发布器单元测试（纯单测，不启动 Spring）
 * <p>
 * 本例只关心"发什么通知、发给谁、内容长什么样"，
 * 投递时机与 MQ 落库属于其他层的职责，不在本用例范围内。
 * </p>
 */
@DisplayName("通知事件发布器")
class NotificationEventPublisherTest {

    private ApplicationEventPublisher applicationEventPublisher;
    private NotificationEventPublisher notificationEventPublisher;

    @BeforeEach
    void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        notificationEventPublisher = new NotificationEventPublisher(applicationEventPublisher);
    }

    @Test
    @DisplayName("待办：收件人为该任务的审批人，内容含流程名与节点名")
    void publishTodoShouldTargetAssignee() {
        notificationEventPublisher.publishTodo(processInstance("请假申请"), task(9L, 7L, "部门经理审批"));

        ApprovalEventMessage event = captureSingleEvent();
        assertEquals(ApprovalEventMessage.TYPE_TODO, event.getType());
        assertEquals(100L, event.getInstanceId());
        assertEquals(9L, event.getTaskId(), "taskId 与 type 构成消费端幂等键，必须是新建任务的主键");
        assertEquals(7L, event.getRecipientId(), "待办应发给当前节点的审批人，而非发起人");
        assertTrue(event.getContent().contains("请假申请"), "内容应带上流程名");
        assertTrue(event.getContent().contains("部门经理审批"), "内容应带上待办节点名");
    }

    @Test
    @DisplayName("驳回：收件人为发起人，内容含审批意见")
    void publishRejectShouldTargetInitiatorWithRemark() {
        notificationEventPublisher.publishReject(processInstance("请假申请"), task(9L, 7L, "部门经理审批"), "材料不全");

        ApprovalEventMessage event = captureSingleEvent();
        assertEquals(ApprovalEventMessage.TYPE_REJECT, event.getType());
        assertEquals(1L, event.getRecipientId(), "驳回应发给流程发起人");
        assertTrue(event.getContent().contains("请假申请"));
        assertTrue(event.getContent().contains("材料不全"), "有审批意见时应拼接进内容");
    }

    @Test
    @DisplayName("驳回：审批意见为空白时内容不拼接意见后缀")
    void publishRejectWithoutRemarkShouldOmitSuffix() {
        notificationEventPublisher.publishReject(processInstance("请假申请"), task(9L, 7L, "部门经理审批"), "   ");

        ApprovalEventMessage event = captureSingleEvent();
        assertFalse(event.getContent().contains("审批意见"), "空白意见不应拼出“审批意见：”");
    }

    @Test
    @DisplayName("通过：收件人为发起人")
    void publishApprovedShouldTargetInitiator() {
        notificationEventPublisher.publishApproved(processInstance("请假申请"), task(9L, 7L, "部门经理审批"));

        ApprovalEventMessage event = captureSingleEvent();
        assertEquals(ApprovalEventMessage.TYPE_APPROVED, event.getType());
        assertEquals(1L, event.getRecipientId(), "通过应发给流程发起人");
        assertEquals(9L, event.getTaskId());
        assertTrue(event.getContent().contains("已审批通过"));
    }

    /**
     * 每个动作都只应登记一条事件，捕获后同时断言"只发了一条"
     */
    private ApprovalEventMessage captureSingleEvent() {
        ArgumentCaptor<ApprovalEventMessage> captor = ArgumentCaptor.forClass(ApprovalEventMessage.class);
        verify(applicationEventPublisher, times(1)).publishEvent(captor.capture());
        return captor.getValue();
    }

    private ProcessInstance processInstance(String definitionName) {
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setId(100L);
        processInstance.setDefinitionName(definitionName);
        processInstance.setInitiatorId(1L);
        return processInstance;
    }

    private Task task(Long taskId, Long assigneeId, String nodeName) {
        Task task = new Task();
        task.setId(taskId);
        task.setInstanceId(100L);
        task.setAssigneeId(assigneeId);
        task.setNodeName(nodeName);
        return task;
    }
}
