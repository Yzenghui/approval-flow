package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.Result;
import com.approval.backend.dto.request.CreateDraftRequest;
import com.approval.backend.dto.request.UpdateDraftRequest;
import com.approval.backend.dto.response.AvailableProcessResponse;
import com.approval.backend.dto.response.CreateDraftResponse;
import com.approval.backend.service.ProcessInstanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class ProcessInstanceController {
    private final ProcessInstanceService processInstanceService;

    public ProcessInstanceController(ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
    }

    /**
     * 获取可用流程列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 可用流程列表
     */
    @GetMapping("/available-definitions")
    @SaCheckLogin
    public Result<?> listAvailableProcesses(
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

        PageResult<AvailableProcessResponse> result = processInstanceService.listAvailableProcesses(pageNum, pageSize, StpUtil.getLoginIdAsLong());

        return Result.success(result);
    }

    /**
     * 新建草稿
     *
     * @param createDraftRequest 新建草稿请求数据传输对象
     * @return 新建草稿响应数据传输对象
     */
    @PostMapping("/drafts")
    @SaCheckLogin
    public Result<?> createDraft(@RequestBody @Valid CreateDraftRequest createDraftRequest) {
        CreateDraftResponse createDraftResponse = processInstanceService.createDraft(createDraftRequest);
        return Result.success(createDraftResponse);
    }

    /**
     * 修改草稿
     *
     * @param instanceId         草稿ID
     * @param updateDraftRequest 修改草稿请求数据传输对象
     */
    @PutMapping("/drafts/{instanceId}")
    @SaCheckLogin
    public Result<?> updateDraft(@PathVariable Long instanceId, @RequestBody @Valid UpdateDraftRequest updateDraftRequest) {
        processInstanceService.updateDraft(instanceId, updateDraftRequest.getFormData());
        return Result.success(null);
    }
}
