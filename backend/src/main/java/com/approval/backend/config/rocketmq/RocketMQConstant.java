package com.approval.backend.config.rocketmq;

/**
 * RocketMQ 相关常量
 */
public final class RocketMQConstant {

    /**
     * 审批事件主题：审批生命周期事件统一发到该 Topic，订阅端 Tag 用 "*"，不做过滤。
     * 事件类型由消息体中的 {@code type} 字段携带（0驳回 1待办 2通过），
     * 消费端统一落库为通知记录，不按类型做分支。
     */
    public static final String APPROVAL_EVENT_TOPIC = "approval-event";

    private RocketMQConstant() {
        // 工具类/常量类不允许实例化
    }
}
