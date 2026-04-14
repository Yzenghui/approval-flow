package com.approval.backend.vo;

import lombok.Data;

/**
 * 流程节点视图对象
 */
@Data
public class ProcessNodeVO {
    private Long id;
    private String nodeName;
    private Integer nodeOrder;
    private String assigneeType;
    private String assigneeExpr;
    private String conditionExpr;
}
