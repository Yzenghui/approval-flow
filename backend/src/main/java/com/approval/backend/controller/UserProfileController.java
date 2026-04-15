package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.Result;
import com.approval.backend.dto.request.UpdatePasswordRequest;
import com.approval.backend.dto.request.UpdateProfileRequest;
import com.approval.backend.service.UserService;
import com.approval.backend.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人信息管理接口
 * 提供获取个人信息、修改邮箱、修改密码等功能
 * 所有接口需要登录即可（无需 ADMIN 角色）
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {
    UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取个人信息
     *
     * @return 个人信息
     */
    @GetMapping("/profile")
    @SaCheckLogin
    public Result<?> getProfile() {
        UserResponse userResponse = userService.getProfile(StpUtil.getLoginIdAsLong());
        return Result.success(userResponse);
    }

    /**
     * 修改个人信息
     *
     * @param updateProfileRequest 修改个人信息请求数据传输对象
     */
    @PatchMapping("/profile")
    @SaCheckLogin
    public Result<?> updateProfile(@RequestBody @Valid UpdateProfileRequest updateProfileRequest) {
        userService.updateProfile(StpUtil.getLoginIdAsLong(), updateProfileRequest);
        return Result.success(null);
    }

    /**
     * 修改个人密码
     *
     * @param updatePasswordRequest 修改个人密码请求数据传输对象
     */
    @PutMapping("/password")
    @SaCheckLogin
    public Result<?> updatePassword(@RequestBody @Valid UpdatePasswordRequest updatePasswordRequest) {
        userService.updatePassword(StpUtil.getLoginIdAsLong(), updatePasswordRequest);
        return Result.success(null);
    }

}
