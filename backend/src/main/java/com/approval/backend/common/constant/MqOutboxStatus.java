package com.approval.backend.common.constant;

import java.util.Set;

/**
 * 本地消息表状态取值
 */
public final class MqOutboxStatus {

    /** 待发送：等待扫表任务投递，包含正在退避等待的 */
    public static final int PENDING = 0;

    /** 已发送：Broker 已接收，不会再被扫描 */
    public static final int SENT = 1;

    /** 发送失败（死信）：重试次数耗尽，不再自动投递，等人工处理 */
    public static final int DEAD = 2;

    private static final Set<Integer> ALL = Set.of(PENDING, SENT, DEAD);

    /**
     * 校验状态取值是否合法
     * <p>
     * 用于接口入参：非法取值应当报错，而不是当成"没有匹配记录"返回空列表，
     * 否则筛选条件写错会被静默吞掉。
     * </p>
     *
     * @param status 待校验的状态值
     * @return 是否为已定义的状态
     */
    public static boolean isValid(Integer status) {
        return status != null && ALL.contains(status);
    }

    private MqOutboxStatus() {
    }
}
