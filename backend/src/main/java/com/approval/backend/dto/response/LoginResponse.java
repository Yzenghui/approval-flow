package com.approval.backend.dto.response;

import lombok.Data;

/**
 * 登录响应数据传输对象
 */
@Data
public class LoginResponse {
    private String token;
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String name;
        private String roleCode;
        private String roleName;


    }
}
