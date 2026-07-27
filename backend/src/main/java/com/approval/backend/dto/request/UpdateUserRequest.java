package com.approval.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 更新用户请求数据传输对象
 */
@Data
public class UpdateUserRequest {
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度必须在2到20个字符之间")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    /** 状态：0禁用 1启用 */
    private Integer status;
}
