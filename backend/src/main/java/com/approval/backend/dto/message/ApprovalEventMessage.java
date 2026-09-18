package com.approval.backend.dto.message;

import lombok.Data;

/**
 * 审批事件消息
 * <p>
 * 一个对象承担两个角色：
 * </p>
 * <ol>
 *   <li><b>本地消息表记录</b>：业务代码在事务内经 {@code NotificationEventPublisher}
 *       把它序列化成 JSON 存入 mq_outbox，与业务写库同事务提交</li>
 *   <li><b>MQ 消息体</b>：{@code MqOutboxScheduler} 扫表补发时把该 JSON 原样
 *       投递到 approval-event 主题</li>
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

    /** 通知类型：TYPE_REJECT(0)驳回 TYPE_TODO(1)待办 TYPE_APPROVED(2)通过 */
    private Integer type;

    /** 收件人ID：驳回/通过为发起人，待办为审批人 */
    private Long recipientId;

    /** 通知内容 */
    private String content;
}
