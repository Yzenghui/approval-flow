package com.approval.backend.service;

import com.approval.backend.dto.request.LoginRequest;
import com.approval.backend.dto.request.RegisterRequest;
import com.approval.backend.dto.response.LoginResponse;
import com.approval.backend.dto.response.RegisterResponse;
import com.approval.backend.entity.User;

/**
 * 认证服务接口
 */
public interface AuthService {
    /**
     * 用户登录验证
     *
     * @param loginRequest 登录请求数据传输对象
     * @return 登录成功返回用户信息
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求数据传输对象
     * @return 注册成功返回注册响应数据传输对象
     */
    RegisterResponse register(RegisterRequest registerRequest);
}
