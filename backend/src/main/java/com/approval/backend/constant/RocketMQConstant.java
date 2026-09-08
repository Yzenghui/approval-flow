package com.approval.backend.constant;

/**
 * RocketMQ 相关常量
 */
public final class RocketMQConstant {

    /**
     * 审批事件主题：所有审批生命周期事件（提交/通过/驳回/撤回）都发到这个 Topic，
     * 通过消息 Tag 区分具体事件类型。
     */
    public static final String APPROVAL_EVENT_TOPIC = "approval-event";

    private RocketMQConstant() {
        // 工具类/常量类不允许实例化
    }
}
