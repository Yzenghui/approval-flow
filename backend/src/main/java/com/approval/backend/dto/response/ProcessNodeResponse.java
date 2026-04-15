package com.approval.backend.dto.response;

import lombok.Data;

/**
 * 流程节点响应数据传输对象
 */
@Data
public class ProcessNodeResponse {
    private Long id;
    private String nodeName;
    private Integer nodeOrder;
    private String assigneeType;
    private String assigneeExpr;
    private String conditionExpr;
}
