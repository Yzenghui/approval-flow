package com.approval.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求数据传输对象
 */
@Data
public class ResetPasswordRequest {
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9_]+$", message = "密码必须同时包含字母和数字")
    private String password;
}
