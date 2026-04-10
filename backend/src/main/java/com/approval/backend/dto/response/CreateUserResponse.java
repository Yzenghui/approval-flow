package com.approval.backend.dto.response;

import lombok.Data;

/**
 * 创建用户响应数据传输对象
 */
@Data
public class CreateUserResponse {
    private Long id;
    private String username;
    private String name;
}

