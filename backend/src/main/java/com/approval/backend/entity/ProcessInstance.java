package com.approval.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例
 */
@Data
@TableName("process_instance")
public class ProcessInstance {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long definitionId;
    private Long initiatorId;
    private String formData;
    private Integer submitCount;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
