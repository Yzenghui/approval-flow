package com.approval.backend.common.constant;

/**
 * 审批记录操作类型取值
 */
public final class ApprovalAction {

    /** 提交：发起人首次提交申请 */
    public static final int SUBMIT = 0;

    /** 通过：审批人同意 */
    public static final int APPROVE = 1;

    /** 驳回：审批人驳回 */
    public static final int REJECT = 2;

    /** 撤回：发起人主动撤回 */
    public static final int WITHDRAW = 3;

    /** 修改重提：驳回/撤回后修改再次提交 */
    public static final int RESUBMIT = 4;

    private ApprovalAction() {
    }
}
