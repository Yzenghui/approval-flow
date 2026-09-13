package com.approval.backend.service.event;

import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.ProcessInstance;
import com.approval.backend.entity.Task;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通知事件发布器
 * <p>
 * 把审批过程中的业务动作翻译成 {@link ApprovalEventMessage} 并登记到 Spring 事件总线。
 * 本类只决定"发什么通知、发给谁"，投递时机与 MQ 投递由 {@code ApprovalEventProducer} 负责。
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

    private final ApplicationEventPublisher applicationEventPublisher;

    public NotificationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
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
     * 组装事件并登记到 Spring 事件总线
     * <p>
     * 只登记不投递：真正发 MQ 的时机由 {@code @TransactionalEventListener(AFTER_COMMIT)} 控制，
     * 保证事务回滚时通知不会发出去。
     * </p>
     */
    private void publish(ProcessInstance processInstance, Task task, int type, Long recipientId, String content) {
        ApprovalEventMessage eventMessage = new ApprovalEventMessage();
        eventMessage.setInstanceId(processInstance.getId());
        eventMessage.setTaskId(task.getId());
        eventMessage.setType(type);
        eventMessage.setRecipientId(recipientId);
        eventMessage.setContent(content);

        applicationEventPublisher.publishEvent(eventMessage);
    }
}
