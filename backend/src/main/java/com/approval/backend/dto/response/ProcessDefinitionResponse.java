package com.approval.backend.dto.response;

import com.approval.backend.common.constant.EnableStatus;
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
    /** 状态：DISABLED(0)禁用 ENABLED(1)启用，取值见 {@link EnableStatus} */
    private Integer status;
    private String formSchema;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
