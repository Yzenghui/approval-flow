package com.approval.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 新增流程请求数据传输对象
 */
@Data
public class CreateProcessRequest {
    @NotBlank(message = "流程名称不能为空")
    private String name;

    @NotBlank(message = "适用角色编码不能为空")
    private String roleCode;

    @NotNull(message = "表单结构不能为空")
    private String formSchema;

    @NotEmpty(message = "至少需要一个审批节点")
    private List<CreateProcessNodeRequest> nodes;
}
