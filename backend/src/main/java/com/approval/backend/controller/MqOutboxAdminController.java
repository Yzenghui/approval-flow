package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.approval.backend.common.Result;
import com.approval.backend.service.event.MqOutboxAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地消息表运维接口
 */
@RestController
@RequestMapping("/api/admin/mq-outbox")
public class MqOutboxAdminController {

    private final MqOutboxAdminService mqOutboxAdminService;

    public MqOutboxAdminController(MqOutboxAdminService mqOutboxAdminService) {
        this.mqOutboxAdminService = mqOutboxAdminService;
    }

    /**
     * 投递概览：各状态条数与积压时间
     *
     * @return 概览数据
     */
    @GetMapping("/overview")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> getOverview() {
        return Result.success(mqOutboxAdminService.overview());
    }

    /**
     * 消息记录分页查询
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param status   状态筛选：0待发送 1已发送 2死信，不传表示不限
     * @param msgKey   消息业务键精确匹配（当前即 taskId），不传表示不限
     * @return 分页记录
     */
    @GetMapping("/records")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    public Result<?> listRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String msgKey
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

        return Result.success(mqOutboxAdminService.listRecords(pageNum, pageSize, status, msgKey));
    }
}
