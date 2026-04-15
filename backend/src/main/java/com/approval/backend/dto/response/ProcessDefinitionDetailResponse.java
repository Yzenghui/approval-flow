package com.approval.backend.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 流程详情响应数据传输对象
 */
@Data
public class ProcessDefinitionDetailResponse extends ProcessDefinitionResponse {
    private List<ProcessNodeResponse> nodes;
}
