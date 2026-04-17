package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.Result;
import com.approval.backend.dto.request.CreateProcessRequest;
import com.approval.backend.dto.response.ProcessDefinitionResponse;
import com.approval.backend.service.ProcessDefinitionService;
import com.approval.backend.dto.response.ProcessDefinitionDetailResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 流程定义控制器
 */
@RestController
@RequestMapping("/api/admin")
public class ProcessDefinitionController {
    private final ProcessDefinitionService processDefinitionService;

    public ProcessDefinitionController(ProcessDefinitionService processDefinitionService) {
        this.processDefinitionService = processDefinitionService;
    }

    /**
     * 获取流程定义列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词（流程名称）
     * @param status   状态
     * @return 流程定义列表
     */
    @GetMapping("/definitions")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> listDefinitions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
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

        PageResult<ProcessDefinitionResponse> result = processDefinitionService.listDefinitions(pageNum, pageSize, keyword, status);

        return Result.success(result);
    }

    /**
     * 获取流程定义详情
     *
     * @param id 流程定义ID
     * @return 流程定义详情
     */
    @GetMapping("/definitions/{id}")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> getDefinitionDetail(@PathVariable Long id) {
        ProcessDefinitionDetailResponse processDefinitionDetailResponse = processDefinitionService.getDefinitionDetail(id);
        return Result.success(processDefinitionDetailResponse);
    }

    /**
     * 新增流程
     *
     * @param createProcessRequest 新增流程请求
     * @return 流程定义响应数据传输对象
     */
    @PostMapping("/definitions")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> createProcess(@RequestBody @Valid CreateProcessRequest createProcessRequest) {
        ProcessDefinitionResponse processDefinitionResponse = processDefinitionService.createProcess(createProcessRequest);
        return Result.success(processDefinitionResponse);
    }

    /**
     * 编辑流程
     *
     * @param id                   流程定义ID
     * @param createProcessRequest 编辑流程请求
     * @return 流程定义响应数据传输对象
     */
    @PatchMapping("/definitions/{id}")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> updateProcess(@PathVariable Long id, @RequestBody @Valid CreateProcessRequest createProcessRequest) {
        ProcessDefinitionResponse processDefinitionResponse = processDefinitionService.updateProcess(id, createProcessRequest);
        return Result.success(processDefinitionResponse);
    }
}
