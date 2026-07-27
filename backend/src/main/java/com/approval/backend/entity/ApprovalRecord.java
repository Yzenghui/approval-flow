package com.approval.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录
 */
@Data
@TableName("approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;
    private Long operatorId;
    private Long taskId;
    /** 操作类型：0提交 1通过 2驳回 3撤回 4修改重提 */
    private Integer action;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}