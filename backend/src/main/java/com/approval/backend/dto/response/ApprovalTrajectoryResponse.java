package com.approval.backend.dto.response;

import com.approval.backend.common.constant.ApprovalAction;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批轨迹响应数据传输对象
 */
@Data
public class ApprovalTrajectoryResponse {
    private String operatorName;
    /** 操作类型：SUBMIT(0)提交 APPROVE(1)通过 REJECT(2)驳回 WITHDRAW(3)撤回 RESUBMIT(4)修改重提，取值见 {@link ApprovalAction} */
    private Integer action;
    private Integer submitRound;
    private String remark;
    private String nodeName;
    private LocalDateTime createTime;
}
