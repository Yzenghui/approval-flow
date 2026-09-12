package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.dto.response.NotificationResponse;

/**
 * 通知服务
 */
public interface NotificationService {

    /**
     * 保存一条通知
     * <p>
     * 幂等：同一个任务的同一种通知只会写入一条，
     * 重复消息直接忽略，不抛异常。
     * </p>
     *
     * @param eventMessage 审批事件消息
     */
    void save(ApprovalEventMessage eventMessage);

    /**
     * 分页查询当前用户的通知
     *
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @param type       通知类型筛选，null 表示不限
     * @param onlyUnread 是否只看未读，null 或 false 表示不限
     * @param userId     当前用户ID（即收件人）
     * @return 分页通知列表
     */
    PageResult<NotificationResponse> getNotifications(Integer pageNum, Integer pageSize, Integer type, Boolean onlyUnread, Long userId);

    /**
     * 统计当前用户的未读通知数
     *
     * @param userId 当前用户ID（即收件人）
     * @return 未读数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 标记单条通知为已读
     * <p>
     * 幂等：已经是已读的通知重复标记不报错。
     * 通知不存在、或存在但不属于当前用户时抛 {@link com.approval.backend.common.BusinessException}，
     * 两种情况共用同一句错误信息，避免被用来探测他人通知是否存在。
     * </p>
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID（即收件人）
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 标记当前用户全部通知为已读
     *
     * @param userId 当前用户ID（即收件人）
     */
    void markAllAsRead(Long userId);
}
