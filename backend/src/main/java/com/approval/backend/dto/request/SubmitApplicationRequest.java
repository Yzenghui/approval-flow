package com.approval.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新建并提交申请请求数据传输对象
 */
@Data
public class SubmitApplicationRequest {
    @NotNull(message = "流程定义ID不能为空")
    private Long definitionId;

    @NotNull(message = "业务数据不能为 null")
    private String formData;
}
