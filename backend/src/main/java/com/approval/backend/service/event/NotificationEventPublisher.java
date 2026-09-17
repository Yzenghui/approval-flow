package com.approval.backend.service.event;

import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.ProcessInstance;
import com.approval.backend.entity.Task;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通知事件发布器
 * <p>
 * 把审批过程中的业务动作翻译成 {@link ApprovalEventMessage} 并登记到本地消息表。
 * 本类只决定"发什么通知、发给谁"，真正投递 MQ 由 {@code MqOutboxScheduler} 扫表驱动。
 * </p>
 * <p>
 * 三类通知的收件人规则：
 * </p>
 * <ul>
 *   <li>待办（type = 1）：新任务的审批人，提醒其处理</li>
 *   <li>驳回（type = 0）：流程发起人</li>
 *   <li>通过（type = 2）：流程发起人，仅在全部节点审批完成后发出</li>
 * </ul>
 */
@Component
public class NotificationEventPublisher {

    private final MqOutboxService mqOutboxService;

    public NotificationEventPublisher(MqOutboxService mqOutboxService) {
        this.mqOutboxService = mqOutboxService;
    }

    /**
     * 发布“您有新的待办”通知
     *
     * @param processInstance 任务所属流程实例
     * @param task            新建的待办任务
     */
    public void publishTodo(ProcessInstance processInstance, Task task) {
        publish(processInstance, task, ApprovalEventMessage.TYPE_TODO, task.getAssigneeId(),
                "您有新的待办：【" + processInstance.getDefinitionName() + "】待您审批，当前节点：" + task.getNodeName());
    }

    /**
     * 发布“申请被驳回”通知
     *
     * @param processInstance 任务所属流程实例
     * @param task            被驳回的任务
     * @param remark          审批意见，可为空
     */
    public void publishReject(ProcessInstance processInstance, Task task, String remark) {
        StringBuilder content = new StringBuilder()
                .append("您的【").append(processInstance.getDefinitionName()).append("】申请已被驳回");
        if (StringUtils.hasText(remark)) {
            content.append("，审批意见：").append(remark);
        }

        publish(processInstance, task, ApprovalEventMessage.TYPE_REJECT,
                processInstance.getInitiatorId(), content.toString());
    }

    /**
     * 发布“申请已通过”通知
     *
     * @param processInstance 任务所属流程实例
     * @param task            最后一个节点的任务
     */
    public void publishApproved(ProcessInstance processInstance, Task task) {
        publish(processInstance, task, ApprovalEventMessage.TYPE_APPROVED, processInstance.getInitiatorId(),
                "您的【" + processInstance.getDefinitionName() + "】申请已审批通过");
    }

    /**
     * 组装事件并登记到本地消息表
     * <p>
     * 写表发生在调用方的事务内：业务回滚则登记一并回滚，
     * 业务提交即意味着消息已被持久化，之后由扫表任务补发。
     * </p>
     */
    private void publish(ProcessInstance processInstance, Task task, int type, Long recipientId, String content) {
        ApprovalEventMessage eventMessage = new ApprovalEventMessage();
        eventMessage.setInstanceId(processInstance.getId());
        eventMessage.setTaskId(task.getId());
        eventMessage.setType(type);
        eventMessage.setRecipientId(recipientId);
        eventMessage.setContent(content);

        mqOutboxService.save(eventMessage);
    }
}