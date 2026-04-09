package com.approval.backend.service;

import com.approval.backend.common.PageResult;
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
}