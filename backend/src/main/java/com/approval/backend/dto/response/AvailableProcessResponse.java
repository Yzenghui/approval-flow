package com.approval.backend.dto.response;

import lombok.Data;

/**
 * 可用流程响应数据传输对象
 */
@Data
public class AvailableProcessResponse {
    private Long id;
    private String name;
    private String roleCode;
    private String formSchema;
}
