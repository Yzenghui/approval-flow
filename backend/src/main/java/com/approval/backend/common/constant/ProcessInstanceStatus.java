package com.approval.backend.common.constant;

/**
 * 流程实例状态取值
 */
public final class ProcessInstanceStatus {

    /** 草稿：已保存未提交，可修改、可删除 */
    public static final int DRAFT = 0;

    /** 审批中：已提交，正在节点间流转 */
    public static final int APPROVING = 1;

    /** 通过：全部节点审批完成，流程结束 */
    public static final int APPROVED = 2;

    /** 驳回：被审批人驳回，可修改后重提 */
    public static final int REJECTED = 3;

    /** 撤回：发起人主动撤回，可修改后重提 */
    public static final int WITHDRAWN = 4;

    private ProcessInstanceStatus() {
    }
}
