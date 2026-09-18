package com.approval.backend.dto.response;

import com.approval.backend.common.constant.ProcessInstanceStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的申请列表响应数据传输对象
 */
@Data
public class MyInstanceResponse {
    private Long id;
    private Long definitionId;
    private String definitionName;
    private Long initiatorId;
    private String formData;
    private Integer submitCount;
    /** 状态：DRAFT(0)草稿 APPROVING(1)审批中 APPROVED(2)通过 REJECTED(3)驳回 WITHDRAWN(4)撤回，取值见 {@link ProcessInstanceStatus} */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
