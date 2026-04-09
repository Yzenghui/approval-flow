package com.approval.backend.mapper;

import com.approval.backend.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * 角色数据访问接口
 */
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID查询角色编码
     *
     * @param userId 用户ID
     * @return 角色编码，如 ADMIN、STAFF、MANAGER 等
     */
    @Select("SELECT r.code FROM user u LEFT JOIN role r ON u.role_id = r.id WHERE u.id = #{userId}")
    String selectRoleCodeByUserId(Long userId);
}
