package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.vo.ProcessDefinitionVO;

/**
 * 流程定义服务接口
 */
public interface ProcessDefinitionService {
    /**
     * 分页查询流程定义列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词（流程名称）
     * @param status   状态
     * @return 分页结果
     */
    PageResult<ProcessDefinitionVO> listDefinitions(Integer pageNum, Integer pageSize, String keyword, Integer status);
}
