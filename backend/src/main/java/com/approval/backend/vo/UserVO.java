package com.approval.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String roleCode;
    private String roleName;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
