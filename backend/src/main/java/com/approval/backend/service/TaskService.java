package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.PendingTaskResponse;

/**
 * 任务服务接口
 */
public interface TaskService {
    /**
     * 获取待办任务
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param userId   用户ID
     * @return 待办任务
     */
    PageResult<PendingTaskResponse> getPendingTasks(Integer pageNum, Integer pageSize, Long userId);
}
