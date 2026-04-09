package com.approval.backend.service.impl;

import com.approval.backend.common.PageResult;
import com.approval.backend.entity.Role;
import com.approval.backend.entity.User;
import com.approval.backend.mapper.RoleMapper;
import com.approval.backend.mapper.UserMapper;
import com.approval.backend.service.UserService;
import com.approval.backend.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务具体实现类
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * 分页查询用户列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param keyword 搜索关键词（用户名、姓名、邮箱）
     * @param roleCode 角色编码筛选
     * @param status 状态筛选
     * @return 分页结果
     */
    @Override
    public PageResult<UserVO> listUsers(Integer pageNum, Integer pageSize, String keyword, String roleCode, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getName, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }

        Long roleId = null;
        if (StringUtils.hasText(roleCode)) {
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode)
            );
            if (role != null) {
                roleId = role.getId();
            }
        }
        if (roleId != null) {
            wrapper.eq(User::getRoleId, roleId);
        }

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> userPage = userMapper.selectPage(page, wrapper);

        if (userPage.getRecords().isEmpty()) {
            PageResult<UserVO> result = new PageResult<>();
            result.setTotal(0L);
            result.setRecords(new ArrayList<>());
            return result;
        }

        List<Long> roleIds = userPage.getRecords()
                .stream()
                .map(User::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Role> roleMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            roleMap = roles.stream().collect(Collectors.toMap(Role::getId, r -> r));
        }

        Map<Long, Role> finalRoleMap = roleMap;

        List<UserVO> records = userPage.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setName(user.getName());
            vo.setEmail(user.getEmail());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            vo.setUpdateTime(user.getUpdateTime());

            Role role = finalRoleMap.get(user.getRoleId());
            if (role != null) {
                vo.setRoleCode(role.getCode());
                vo.setRoleName(role.getName());
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<UserVO> result = new PageResult<>();
        result.setTotal(userPage.getTotal());
        result.setRecords(records);
        return result;
    }
}