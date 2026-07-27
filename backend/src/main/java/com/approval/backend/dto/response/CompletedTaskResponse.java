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
    /** 操作类型：0提交 1通过 2驳回 3撤回 4修改重提 */
    private Integer action;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
