package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.Result;
import com.approval.backend.dto.request.ApproveRequest;
import com.approval.backend.dto.request.RejectRequest;
import com.approval.backend.dto.response.PendingTaskResponse;
import com.approval.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 任务控制器
 */
@RestController
@RequestMapping("/api/user")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 获取待办任务
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 待办任务
     */
    @GetMapping("/tasks/pending")
    @SaCheckLogin
    public Result<?> getPendingTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        PageResult<PendingTaskResponse> result = taskService.getPendingTasks(pageNum, pageSize, StpUtil.getLoginIdAsLong());

        return Result.success(result);
    }

    /**
     * 通过任务
     *
     * @param taskId         任务ID
     * @param approveRequest 审批信息
     */
    @PostMapping("/tasks/{taskId}/approve")
    @SaCheckLogin
    public Result<?> approveTask(@PathVariable Long taskId, @RequestBody @Valid ApproveRequest approveRequest) {
        taskService.approveTask(taskId, approveRequest.getRemark(), StpUtil.getLoginIdAsLong());
        return Result.success(null);
    }


    /**
     * 驳回任务
     *
     * @param taskId        任务ID
     * @param rejectRequest 驳回信息
     */
    @PostMapping("/tasks/{taskId}/reject")
    @SaCheckLogin
    public Result<?> rejectTask(@PathVariable Long taskId, @RequestBody @Valid RejectRequest rejectRequest) {
        taskService.rejectTask(taskId, rejectRequest.getRemark(), StpUtil.getLoginIdAsLong());
        return Result.success(null);
    }
}
