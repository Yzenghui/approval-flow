package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.RejectRequest;
import com.approval.backend.dto.response.CompletedTaskResponse;
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

    /**
     * 通过任务
     *
     * @param taskId 任务ID
     * @param remark 审批意见
     * @param userId 用户ID
     */
    void approveTask(Long taskId, String remark, Long userId);

    /**
     * 驳回任务
     *
     * @param taskId 任务ID
     * @param remark 审批意见
     * @param userId 用户ID
     */
    void rejectTask(Long taskId, String remark, Long userId);

    /**
     * 获取已办任务
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param userId   用户ID
     * @return 已办任务
     */
    PageResult<CompletedTaskResponse> getCompletedTasks(Integer pageNum, Integer pageSize, Long userId);
}
