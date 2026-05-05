package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的申请列表响应数据传输对象
 */
@Data
public class MyInstanceResponse {
    private Long id;
    private Long definitionId;
    private String definitionName;
    private String formData;
    private Integer submitCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
