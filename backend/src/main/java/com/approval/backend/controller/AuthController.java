package com.approval.backend.controller;

import com.approval.backend.common.Result;
import com.approval.backend.dto.request.LoginRequest;
import com.approval.backend.dto.request.RegisterRequest;
import com.approval.backend.dto.response.LoginResponse;
import com.approval.backend.dto.response.RegisterResponse;
import com.approval.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录
     * @param loginRequest 登录请求数据传输对象
     * @return 登录成功返回用户信息
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return Result.success(response);
    }

    /**
     *  注册
     * @param registerRequest 注册请求数据传输对象
     * @return 注册成功返回用户信息
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody @Valid RegisterRequest registerRequest) {
        RegisterResponse response = authService.register(registerRequest);
        return Result.success(response);
    }
}
