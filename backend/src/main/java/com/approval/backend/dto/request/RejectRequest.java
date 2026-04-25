package com.approval.backend.dto.request;

import lombok.Data;

/**
 * 驳回任务请求数据传输对象
 */
@Data
public class RejectRequest {
    private String remark;
}
