package com.approval.backend.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.constant.EnableStatus;
import com.approval.backend.dto.request.*;
import com.approval.backend.dto.response.CreateUserResponse;
import com.approval.backend.entity.Role;
import com.approval.backend.entity.User;
import com.approval.backend.mapper.RoleMapper;
import com.approval.backend.mapper.UserMapper;
import com.approval.backend.service.UserService;
import com.approval.backend.dto.response.UserResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词（用户名、姓名、邮箱）
     * @param roleCode 角色编码筛选
     * @param status   状态筛选
     * @return 分页结果
     */
    @Override
    public PageResult<UserResponse> listUsers(Integer pageNum, Integer pageSize, String keyword, String roleCode, Integer status) {
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
            Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
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
            PageResult<UserResponse> result = new PageResult<>();
            result.setTotal(0L);
            result.setRecords(new ArrayList<>());
            return result;
        }

        List<Long> roleIds = userPage.getRecords().stream()
                .map(User::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Role> roleMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            roleMap = roles.stream().collect(Collectors.toMap(Role::getId, r -> r));
        }

        Map<Long, Role> finalRoleMap = roleMap;

        List<UserResponse> records = userPage.getRecords().stream().map(user -> {
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setUsername(user.getUsername());
            userResponse.setName(user.getName());
            userResponse.setEmail(user.getEmail());
            userResponse.setManagerId(user.getManagerId());
            userResponse.setStatus(user.getStatus());
            userResponse.setCreateTime(user.getCreateTime());
            userResponse.setUpdateTime(user.getUpdateTime());

            Role role = finalRoleMap.get(user.getRoleId());
            if (role != null) {
                userResponse.setRoleCode(role.getCode());
                userResponse.setRoleName(role.getName());
            }
            return userResponse;
        }).collect(Collectors.toList());

        PageResult<UserResponse> result = new PageResult<>();
        result.setTotal(userPage.getTotal());
        result.setRecords(records);
        return result;
    }

    /**
     * 创建用户
     *
     * @param createUserRequest 创建用户请求数据传输对象
     * @return 创建成功返回用户信息
     */
    @Override
    public CreateUserResponse createUser(CreateUserRequest createUserRequest) {
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, createUserRequest.getUsername()));
        if (existingUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User existingEmail = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, createUserRequest.getEmail()));
        if (existingEmail != null) {
            throw new BusinessException(400, "邮箱已存在");
        }

        User user = new User();
        user.setUsername(createUserRequest.getUsername());
        user.setPassword(BCrypt.hashpw(createUserRequest.getPassword(), BCrypt.gensalt()));
        user.setName(createUserRequest.getName());
        user.setEmail(createUserRequest.getEmail());

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, createUserRequest.getRoleCode()));
        if (role == null) {
            throw new BusinessException(400, "角色编码无效");
        }
        user.setRoleId(role.getId());

        user.setManagerId(null);
        user.setStatus(EnableStatus.ENABLED);

        userMapper.insert(user);

        CreateUserResponse response = new CreateUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());

        return response;
    }

    /**
     * 更新用户
     *
     * @param id                用户ID
     * @param updateUserRequest 更新用户请求数据传输对象
     * @param currentUserId     当前登录用户ID
     */
    @Override
    public void updateUser(Long id, UpdateUserRequest updateUserRequest, Long currentUserId) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        User existingEmail = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, updateUserRequest.getEmail())
                .ne(User::getId, id));
        if (existingEmail != null) {
            throw new BusinessException(400, "邮箱已存在");
        }

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, updateUserRequest.getRoleCode()));
        if (role == null) {
            throw new BusinessException(400, "角色编码无效");
        }
        Long newRoleId = role.getId();
        Long oldRoleId = user.getRoleId();

        if (id.equals(currentUserId) && !oldRoleId.equals(newRoleId)) {
            throw new BusinessException(400, "不能修改自己的角色");
        }

        Integer oldStatus = user.getStatus();

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getName, updateUserRequest.getName())
                .set(User::getEmail, updateUserRequest.getEmail())
                .set(User::getRoleId, newRoleId)
                .set(User::getStatus, updateUserRequest.getStatus())
                .set(User::getUpdateTime, LocalDateTime.now())
        );

        if (oldStatus == EnableStatus.ENABLED && updateUserRequest.getStatus() == EnableStatus.DISABLED) {
            StpUtil.kickout(id);
        }
    }

    /**
     * 重置密码
     *
     * @param id                   用户ID
     * @param resetPasswordRequest 重置密码请求数据传输对象
     */
    @Override
    public void resetPassword(Long id, ResetPasswordRequest resetPasswordRequest) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getPassword, BCrypt.hashpw(resetPasswordRequest.getPassword(), BCrypt.gensalt()))
                .set(User::getUpdateTime, LocalDateTime.now())
        );
    }

    /**
     * 修改上级
     *
     * @param id                   用户ID
     * @param updateManagerRequest 修改上级请求数据传输对象
     */
    @Override
    public void updateManager(Long id, UpdateManagerRequest updateManagerRequest) {
        User currentUser = userMapper.selectById(id);
        if (currentUser == null) {
            throw new BusinessException(400, "用户不存在");
        }

        if (updateManagerRequest.getManagerId() == null) {
            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                    .eq(User::getId, id)
                    .set(User::getManagerId, null)
                    .set(User::getUpdateTime, LocalDateTime.now());
            userMapper.update(null, wrapper);
            return;
        }

        User managerUser = userMapper.selectById(updateManagerRequest.getManagerId());
        if (managerUser == null) {
            throw new BusinessException(400, "上级用户不存在");
        }

        if (currentUser.getId().equals(updateManagerRequest.getManagerId())) {
            throw new BusinessException(400, "不能设置自己为上级");
        }

        Role currentRole = roleMapper.selectById(currentUser.getRoleId());
        Role targetRole = roleMapper.selectById(managerUser.getRoleId());
        int currentLevel = currentRole.getLevel();
        int targetLevel = targetRole.getLevel();
        if (currentLevel >= targetLevel) {
            throw new BusinessException(400, "上级角色等级必须高于当前用户");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getManagerId, updateManagerRequest.getManagerId())
                .set(User::getUpdateTime, LocalDateTime.now())
        );
    }

    /**
     * 获取个人信息
     *
     * @param id 用户ID
     * @return 个人信息
     */
    @Override
    public UserResponse getProfile(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        UserResponse userResponse = new UserResponse();

        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setName(user.getName());
        userResponse.setEmail(user.getEmail());
        userResponse.setRoleCode(roleMapper.selectRoleCodeByUserId(id));
        userResponse.setRoleName(roleMapper.selectRoleNameByUserId(id));
        userResponse.setManagerId(user.getManagerId());
        userResponse.setStatus(user.getStatus());
        userResponse.setCreateTime(user.getCreateTime());
        userResponse.setUpdateTime(user.getUpdateTime());

        return userResponse;
    }

    /**
     * 修改个人信息
     *
     * @param id                   用户ID
     * @param updateProfileRequest 修改个人信息请求数据传输对象
     */
    @Override
    public void updateProfile(Long id, UpdateProfileRequest updateProfileRequest) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        User existingEmail = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, updateProfileRequest.getEmail()).ne(User::getId, id));
        if (existingEmail != null) {
            throw new BusinessException(400, "邮箱已存在");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getEmail, updateProfileRequest.getEmail())
                .set(User::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 修改个人密码
     *
     * @param id                    用户ID
     * @param updatePasswordRequest 修改个人密码请求数据传输对象
     */
    @Override
    public void updatePassword(Long id, UpdatePasswordRequest updatePasswordRequest) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        if (!BCrypt.checkpw(updatePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        if (updatePasswordRequest.getNewPassword().equals(updatePasswordRequest.getOldPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getPassword, BCrypt.hashpw(updatePasswordRequest.getNewPassword(), BCrypt.gensalt()))
                .set(User::getUpdateTime, LocalDateTime.now())
        );
    }
}