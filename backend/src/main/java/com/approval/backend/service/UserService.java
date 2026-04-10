package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.CreateUserRequest;
import com.approval.backend.dto.response.CreateUserResponse;
import com.approval.backend.vo.UserVO;

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
    PageResult<UserVO> listUsers(Integer pageNum, Integer pageSize, String keyword, String roleCode, Integer status);

    /**
     * 创建用户
     * @param createUserRequest 创建用户请求数据传输对象
     * @return 创建成功返回用户信息
     */
    CreateUserResponse createUser(CreateUserRequest createUserRequest);
}