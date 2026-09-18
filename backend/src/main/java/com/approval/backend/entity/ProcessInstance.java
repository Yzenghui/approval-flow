package com.approval.backend.entity;

import com.approval.backend.common.constant.ProcessInstanceStatus;
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
    /** 状态：DRAFT(0)草稿 APPROVING(1)审批中 APPROVED(2)通过 REJECTED(3)驳回 WITHDRAWN(4)撤回，取值见 {@link ProcessInstanceStatus} */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
