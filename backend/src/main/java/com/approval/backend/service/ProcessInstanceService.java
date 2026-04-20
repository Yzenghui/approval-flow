package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.AvailableProcessResponse;

/**
 * 流程实例服务接口
 */
public interface ProcessInstanceService {
    /**
     * 获取可用流程列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param id       用户ID
     * @return 可用流程列表
     */
    PageResult<AvailableProcessResponse> listAvailableProcesses(Integer pageNum, Integer pageSize, Long id);
}
