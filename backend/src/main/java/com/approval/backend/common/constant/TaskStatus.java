package com.approval.backend.common.constant;

/**
 * 审批任务状态取值
 */
public final class TaskStatus {

    /** 待处理：任务已生成，等待审批人操作 */
    public static final int PENDING = 0;

    /** 已处理：审批人已通过或驳回 */
    public static final int COMPLETED = 1;

    /** 已取消：实例被撤回/驳回后连带取消，不再可操作 */
    public static final int CANCELLED = 2;

    private TaskStatus() {
    }
}
