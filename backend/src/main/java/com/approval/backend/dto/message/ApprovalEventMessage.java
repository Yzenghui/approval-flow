package com.approval.backend.dto.message;

import lombok.Data;

/**
 * 审批事件消息
 * <p>
 * 一个对象承担两个角色：
 * </p>
 * <ol>
 *   <li><b>Spring 事务事件</b>：业务代码在事务内 publishEvent 登记事件</li>
 *   <li><b>MQ 消息体</b>：{@code ApprovalEventProducer} 在事务提交后
 *       把它序列化成 JSON 投递到 approval-event 主题</li>
 * </ol>
 */
@Data
public class ApprovalEventMessage {

    /** 通知类型：申请被驳回，收件人为流程发起人 */
    public static final int TYPE_REJECT = 0;

    /** 通知类型：待办提醒，收件人为当前节点的审批人 */
    public static final int TYPE_TODO = 1;

    /** 通知类型：申请已通过，收件人为流程发起人 */
    public static final int TYPE_APPROVED = 2;

    /** 流程实例ID */
    private Long instanceId;

    /** 触发本次通知的任务ID，与 type 共同构成消费端幂等键（uk_task_type） */
    private Long taskId;

    /** 通知类型：0驳回 1待办 2通过 */
    private Integer type;

    /** 收件人ID：驳回/通过为发起人，待办为审批人 */
    private Long recipientId;

    /** 通知内容 */
    private String content;
}
