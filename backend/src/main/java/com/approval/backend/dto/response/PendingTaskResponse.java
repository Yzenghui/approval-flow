package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务响应数据传输对象
 */
@Data
public class PendingTaskResponse {
    private Long taskId;
    private Long instanceId;
    private String definitionName;
    private String nodeName;
    private String initiatorName;
    private String formData;
    private LocalDateTime createTime;
}
