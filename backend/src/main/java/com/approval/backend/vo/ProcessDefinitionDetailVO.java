package com.approval.backend.vo;

import lombok.Data;

import java.util.List;

/**
 * 流程详情视图对象
 */
@Data
public class ProcessDefinitionDetailVO extends ProcessDefinitionVO {
    private List<ProcessNodeVO> nodes;
}
