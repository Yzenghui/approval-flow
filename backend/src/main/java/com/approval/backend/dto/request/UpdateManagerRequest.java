package com.approval.backend.dto.request;

import lombok.Data;

/**
 * 修改上级请求数据传输对象
 */
@Data
public class UpdateManagerRequest {
    private Long managerId;
}
