package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应数据传输对象
 */
@Data
public class UserResponse {
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
