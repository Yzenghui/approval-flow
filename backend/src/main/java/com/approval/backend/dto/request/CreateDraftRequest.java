package com.approval.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新建草稿请求数据传输对象
 */
@Data
public class CreateDraftRequest {
    @NotNull(message = "流程定义ID不能为空")
    private Long definitionId;

    @NotNull(message = "业务数据不能为 null")
    private String formData;
}
