package com.approval.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.Result;
import com.approval.backend.dto.response.NotificationResponse;
import com.approval.backend.service.NotificationService;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/user")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 获取通知列表
     *
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @param type       通知类型筛选：0驳回 1待办 2通过，不传表示不限
     * @param onlyUnread 是否只看未读，不传表示不限
     * @return 分页通知列表
     */
    @GetMapping("/notifications")
    @SaCheckLogin
    public Result<?> getNotifications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Boolean onlyUnread
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

        PageResult<NotificationResponse> result = notificationService.getNotifications(
                pageNum, pageSize, type, onlyUnread, StpUtil.getLoginIdAsLong());

        return Result.success(result);
    }

    /**
     * 获取未读通知数
     *
     * @return 未读数量
     */
    @GetMapping("/notifications/unread-count")
    @SaCheckLogin
    public Result<?> getUnreadCount() {
        return Result.success(notificationService.getUnreadCount(StpUtil.getLoginIdAsLong()));
    }

    /**
     * 标记单条通知为已读
     *
     * @param notificationId 通知ID
     */
    @PutMapping("/notifications/{notificationId}/read")
    @SaCheckLogin
    public Result<?> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId, StpUtil.getLoginIdAsLong());
        return Result.success(null);
    }

    /**
     * 标记全部通知为已读
     */
    @PutMapping("/notifications/read-all")
    @SaCheckLogin
    public Result<?> markAllAsRead() {
        notificationService.markAllAsRead(StpUtil.getLoginIdAsLong());
        return Result.success(null);
    }
}
