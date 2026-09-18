package com.approval.backend.entity;

import com.approval.backend.common.constant.EnableStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("user")
public class User {
    @TableId(type= IdType.AUTO)
    private Long id;

    private String username;
    private String password; // 加密后的密码
    private String name;
    private String email;
    private Long roleId;
    private Long managerId;
    /** 状态：DISABLED(0)禁用 ENABLED(1)启用，取值见 {@link EnableStatus} */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
