package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.*;
import com.approval.backend.dto.response.CreateUserResponse;
import com.approval.backend.dto.response.UserResponse;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 分页查询用户列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 搜索关键词（用户名、姓名、邮箱）
     * @param roleCode 角色编码筛选
     * @param status 状态筛选
     * @return 分页结果
     */
    PageResult<UserResponse> listUsers(Integer pageNum, Integer pageSize, String keyword, String roleCode, Integer status);

    /**
     * 创建用户
     * @param createUserRequest 创建用户请求数据传输对象
     * @return 创建成功返回用户信息
     */
    CreateUserResponse createUser(CreateUserRequest createUserRequest);

    /**
     * 更新用户
     * @param id 用户ID
     * @param updateUserRequest 更新用户请求数据传输对象
     */
    void updateUser(Long id, UpdateUserRequest updateUserRequest);

    /**
     * 重置用户密码
     * @param id 用户ID
     * @param request 重置密码请求数据传输对象
     */
    void resetPassword(Long id, ResetPasswordRequest request);

    /**
     * 修改上级
     * @param id 用户ID
     * @param updateManagerRequest 修改上级请求数据传输对象
     */
    void updateManager(Long id, UpdateManagerRequest updateManagerRequest);

    /**
     * 获取个人信息
     * @param id 用户ID
     * @return 个人信息
     */
    UserResponse getProfile(Long id);

    /**
     * 修改个人信息
     * @param id 用户ID
     * @param updateProfileRequest 修改个人信息请求数据传输对象
     */
    void updateProfile(Long id, UpdateProfileRequest updateProfileRequest);

    /**
     * 修改个人密码
     * @param id 用户ID
     * @param updatePasswordRequest 修改个人密码请求数据传输对象
     */
    void updatePassword(Long id,UpdatePasswordRequest updatePasswordRequest);
}