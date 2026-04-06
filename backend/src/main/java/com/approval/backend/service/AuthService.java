package com.approval.backend.service;

import com.approval.backend.dto.request.LoginRequest;
import com.approval.backend.dto.response.LoginResponse;
import com.approval.backend.entity.User;

/**
 * 认证服务接口
 */
public interface AuthService {
    /**
     * 用户登录验证
     *
     * @param loginRequest 登录请求数据传输对象
     * @return 登录成功返回用户信息，失败返回 null
     */
    LoginResponse login(LoginRequest loginRequest);

}
