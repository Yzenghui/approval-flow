package com.approval.backend.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 流程实例详情响应数据传输对象
 */
@Data
public class ProcessInstanceDetailResponse extends MyInstanceResponse {
    private List<ApprovalTrajectoryResponse> records;
}
