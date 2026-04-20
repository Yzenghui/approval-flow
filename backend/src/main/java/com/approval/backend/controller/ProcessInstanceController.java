package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.Result;
import com.approval.backend.dto.response.AvailableProcessResponse;
import com.approval.backend.service.ProcessInstanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class ProcessInstanceController {
    private final ProcessInstanceService processInstanceService;

    public ProcessInstanceController(ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
    }

    /**
     * 获取可用流程列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 可用流程列表
     */
    @GetMapping("/available-definitions")
    @SaCheckLogin
    public Result<?> listAvailableProcesses(
            @RequestParam (defaultValue = "1") Integer pageNum,
            @RequestParam (defaultValue = "10") Integer pageSize
    ){
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

}
