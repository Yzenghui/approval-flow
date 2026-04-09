package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.Result;
import com.approval.backend.service.UserService;
import com.approval.backend.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 搜索关键词（用户名、姓名、邮箱）
     * @param roleCode 角色编码筛选
     * @param status 状态筛选
     * @return 分页结果
     */
    @GetMapping("/users")
    @SaCheckLogin
    @SaCheckRole("admin")
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
}
