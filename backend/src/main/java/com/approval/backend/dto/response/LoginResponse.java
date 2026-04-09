package com.approval.backend.dto.response;

import com.approval.backend.vo.UserVO;
import lombok.Data;

/**
 * 登录响应数据传输对象
 */
@Data
public class LoginResponse {
    private String token;
    private UserVO userInfo;
}
