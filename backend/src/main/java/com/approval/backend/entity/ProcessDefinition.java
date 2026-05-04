package com.approval.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义
 */
@Data
@TableName("process_definition")
public class ProcessDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer version;
    private String roleCode;
    private Integer status;
    private String formSchema;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
