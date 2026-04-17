package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义响应数据传输对象
 */
@Data
public class ProcessDefinitionResponse {
    private Long id;
    private String name;
    private Integer version;
    private String roleCode;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
