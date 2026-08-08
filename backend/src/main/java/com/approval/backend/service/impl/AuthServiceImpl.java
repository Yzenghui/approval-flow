package com.approval.backend.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.BusinessException;
import com.approval.backend.dto.request.LoginRequest;
import com.approval.backend.dto.request.RegisterRequest;
import com.approval.backend.dto.response.LoginResponse;
import com.approval.backend.dto.response.RegisterResponse;
import com.approval.backend.entity.Role;
import com.approval.backend.entity.User;
import com.approval.backend.mapper.RoleMapper;
import com.approval.backend.mapper.UserMapper;
import com.approval.backend.service.AuthService;
import com.approval.backend.dto.response.UserResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务的具体实现类
 */
@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 5;
    private static final String FAIL_KEY_PREFIX = "login:fail:";

    public AuthServiceImpl(UserMapper userMapper, RoleMapper roleMapper, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 用户登录验证
     *
     * @param loginRequest 登录请求数据传输对象
     * @return 登录成功返回用户信息
     * @throws BusinessException 用户名或密码错误时抛出401，账号禁用时抛出403
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String failKey = FAIL_KEY_PREFIX + loginRequest.getUsername();

        String failCountStr = redisTemplate.opsForValue().get(failKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= MAX_FAIL_COUNT) {
            throw new BusinessException(400, "账号已锁定，请5分钟后重试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, loginRequest.getUsername())
        );

        if (user == null || !BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            Long failCount = redisTemplate.opsForValue().increment(failKey);
            if (failCount != null && failCount == 1) {
                redisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            if (failCount != null && failCount >= MAX_FAIL_COUNT) {
                throw new BusinessException(400, "账号已锁定，请5分钟后重试");
            }
            int remaining = MAX_FAIL_COUNT - (failCount != null ? failCount.intValue() : 0);
            throw new BusinessException(400, "用户名或密码错误，您还有" + remaining + "次尝试机会");
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        redisTemplate.delete(failKey);

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        LoginResponse response = new LoginResponse();
        response.setToken(token);

        UserResponse userInfo = new UserResponse();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setName(user.getName());
        userInfo.setEmail(user.getEmail());

        Long roleId = user.getRoleId();
        Role role = roleMapper.selectById(roleId);
        userInfo.setRoleCode(role.getCode());
        userInfo.setRoleName(role.getName());

        userInfo.setManagerId(user.getManagerId());
        userInfo.setStatus(user.getStatus());
        userInfo.setCreateTime(user.getCreateTime());
        userInfo.setUpdateTime(user.getUpdateTime());

        response.setUserInfo(userInfo);

        return response;
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求数据传输对象
     * @return 注册成功返回用户信息
     * @throws BusinessException 用户名或邮箱已存在时抛出400
     */
    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, registerRequest.getUsername())
        );
        if (existingUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User existingEmail = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, registerRequest.getEmail())
        );
        if (existingEmail != null) {
            throw new BusinessException(400, "邮箱已存在");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt()));
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());

        Role staffRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, "STAFF")
        );
        if (staffRole == null) {
            throw new BusinessException(500, "系统内部错误，请稍后重试");
        }
        Long roleId = staffRole.getId();

        user.setRoleId(roleId);
        user.setManagerId(null);
        user.setStatus(1);

        userMapper.insert(user);

        RegisterResponse response = new RegisterResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());

        return response;
    }

}
