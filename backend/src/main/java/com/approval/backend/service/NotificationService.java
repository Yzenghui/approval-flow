package com.approval.backend.service;

import com.approval.backend.dto.message.ApprovalEventMessage;

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
}
