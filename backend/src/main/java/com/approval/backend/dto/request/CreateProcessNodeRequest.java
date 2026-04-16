package com.approval.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 新增流程节点请求数据传输对象
 */
@Data
public class CreateProcessNodeRequest {
    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    @NotNull(message = "节点顺序不能为空")
    @Min(value = 1, message = "节点顺序最小为1")
    private Integer nodeOrder;

    @NotBlank(message = "分配策略不能为空")
    @Pattern(regexp = "RELATIVE|USER|ROLE", message = "分配策略无效")
    private String assigneeType;

    @NotBlank(message = "分配目标值不能为空")
    private String assigneeExpr;

    private String conditionExpr;
}
