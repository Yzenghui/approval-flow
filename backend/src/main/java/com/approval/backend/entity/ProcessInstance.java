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
    private String definitionName;
    private Long initiatorId;
    private String formData;
    private Integer submitCount;
    /** 状态：0草稿 1审批中 2通过 3驳回 4撤回 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
