package com.approval.backend.entity;

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
    /** 状态：0禁用 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
