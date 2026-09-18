package com.approval.backend.dto.response;

import com.approval.backend.common.constant.ApprovalAction;
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
    /** 操作类型：SUBMIT(0)提交 APPROVE(1)通过 REJECT(2)驳回 WITHDRAW(3)撤回 RESUBMIT(4)修改重提，取值见 {@link ApprovalAction} */
    private Integer action;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
