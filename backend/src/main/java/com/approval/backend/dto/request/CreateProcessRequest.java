package com.approval.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 新增流程请求数据传输对象
 */
@Data
public class CreateProcessRequest {
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @NotEmpty(message = "至少需要一个审批节点")
    private List<CreateProcessNodeRequest> nodes;
}
