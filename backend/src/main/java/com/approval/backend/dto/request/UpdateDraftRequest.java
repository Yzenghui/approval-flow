package com.approval.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改草稿请求数据传输对象
 */
@Data
public class UpdateDraftRequest {
    @NotNull(message = "业务数据不能为 null")
    private String formData;
}
