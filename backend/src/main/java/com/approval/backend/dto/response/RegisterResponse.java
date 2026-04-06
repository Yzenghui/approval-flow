package com.approval.backend.dto.response;

import lombok.Data;

/**
 * 注册响应数据传输对象
 */
@Data
public class RegisterResponse {
    private Long id;
    private String username;
    private String name;
}
