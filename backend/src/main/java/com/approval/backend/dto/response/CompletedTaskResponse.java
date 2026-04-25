package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已办任务响应数据传输对象
 */
@Data
public class CompletedTaskResponse {
    private Long taskId;
    private Long instanceId;
    private String definitionName;
    private String nodeName;
    private String initiatorName;
    private String formData;
    private Integer action;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
