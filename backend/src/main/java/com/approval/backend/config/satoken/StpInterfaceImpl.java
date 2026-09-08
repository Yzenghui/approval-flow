package com.approval.backend.config.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.approval.backend.mapper.RoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    private RoleMapper roleMapper;

    public StpInterfaceImpl(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        String roleCode = roleMapper.selectRoleCodeByUserId(userId);
        return roleCode == null ? List.of() : List.of(roleCode);
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }
}