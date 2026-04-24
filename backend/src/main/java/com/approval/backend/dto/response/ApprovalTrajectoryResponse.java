package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批轨迹响应数据传输对象
 */
@Data
public class ApprovalTrajectoryResponse {
    private String operatorName;
    private Integer action;
    private Integer submitRound;
    private String remark;
    private String nodeName;
    private LocalDateTime createTime;
}
