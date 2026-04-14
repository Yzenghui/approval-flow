package com.approval.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点
 */
@Data
@TableName("process_node")
public class ProcessNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long definitionId;
    private String nodeName;
    private Integer nodeOrder;
    private String assigneeType;
    private String assigneeExpr;
    private String conditionExpr;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
