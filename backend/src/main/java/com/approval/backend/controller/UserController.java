package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.Result;
import com.approval.backend.dto.request.CreateUserRequest;
import com.approval.backend.dto.request.UpdateUserRequest;
import com.approval.backend.dto.response.CreateUserResponse;
import com.approval.backend.service.UserService;
import com.approval.backend.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/admin")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词（用户名、姓名、邮箱）
     * @param roleCode 角色编码筛选
     * @param status   状态筛选
     * @return 分页结果
     */
    @GetMapping("/users")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer status
    ) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        PageResult<UserVO> result = userService.listUsers(pageNum, pageSize, keyword, roleCode, status);
        return Result.success(result);
    }

    /**
     * 创建用户
     *
     * @param createUserRequest 创建用户请求数据传输对象
     * @return 创建成功返回用户信息
     */
    @PostMapping("/users")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        CreateUserResponse response = userService.createUser(createUserRequest);
        return Result.success(response);
    }

    /**
     * 更新用户
     *
     * @param id
     * @param updateUserRequest 更新用户请求数据传输对象
     */
    @PatchMapping("/users/{id}")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        userService.updateUser(id, updateUserRequest);
        return Result.success(null);
    }
}
