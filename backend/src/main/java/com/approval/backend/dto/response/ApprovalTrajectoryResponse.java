package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批轨迹响应数据传输对象
 */
@Data
public class ApprovalTrajectoryResponse {
    private String operatorName;
    /** 操作类型：0提交 1通过 2驳回 3撤回 4修改重提 */
    private Integer action;
    private Integer submitRound;
    private String remark;
    private String nodeName;
    private LocalDateTime createTime;
}
