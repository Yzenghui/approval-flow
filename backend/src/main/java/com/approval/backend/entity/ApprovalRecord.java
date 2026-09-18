package com.approval.backend.entity;

import com.approval.backend.common.constant.ApprovalAction;
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
    /** 操作类型：SUBMIT(0)提交 APPROVE(1)通过 REJECT(2)驳回 WITHDRAW(3)撤回 RESUBMIT(4)修改重提，取值见 {@link ApprovalAction} */
    private Integer action;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}