package com.approval.backend.dto.response;

import com.approval.backend.common.constant.EnableStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应数据传输对象
 */
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String roleCode;
    private String roleName;
    private Long managerId;
    /** 状态：DISABLED(0)禁用 ENABLED(1)启用，取值见 {@link EnableStatus} */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
