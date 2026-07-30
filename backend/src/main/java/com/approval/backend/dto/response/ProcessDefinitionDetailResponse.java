package com.approval.backend.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流程详情响应数据传输对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessDefinitionDetailResponse extends ProcessDefinitionResponse {
    private List<ProcessNodeResponse> nodes;
}
