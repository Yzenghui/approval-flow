package com.approval.backend.entity;

import com.approval.backend.common.constant.EnableStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色
 */
@Data
@TableName("role")
public class Role {
    @TableId(type= IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private Integer level;
    /** 状态：DISABLED(0)禁用 ENABLED(1)启用，取值见 {@link EnableStatus} */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}

