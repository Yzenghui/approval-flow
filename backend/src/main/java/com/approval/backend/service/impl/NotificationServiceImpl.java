package com.approval.backend.service.impl;

import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.Notification;
import com.approval.backend.mapper.NotificationMapper;
import com.approval.backend.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现类
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 保存一条通知（幂等）
     * <p>
     * RocketMQ 只保证"至少一次投递"，消息可能重复到达，
     * 这里不采用"先查再插"（两个并发线程会同时查不到然后都插入），
     * 而是直接把唯一键 uk_task_type(task_id, type) 交给数据库兜底：
     * 重复消息会撞唯一键抛 {@link DuplicateKeyException}，
     * 捕获后按"已处理"处理即可，天然没有并发窗口。
     * </p>
     *
     * @param eventMessage 审批事件消息
     */
    @Override
    public void save(ApprovalEventMessage eventMessage) {
        Notification notification = new Notification();
        notification.setInstanceId(eventMessage.getInstanceId());
        notification.setTaskId(eventMessage.getTaskId());
        notification.setRecipientId(eventMessage.getRecipientId());
        notification.setType(eventMessage.getType());
        notification.setContent(eventMessage.getContent());
        notification.setIsRead(0);

        try {
            notificationMapper.insert(notification);
            log.info("通知已落库: id={}, instanceId={}, taskId={}, type={}, recipientId={}",
                    notification.getId(),
                    notification.getInstanceId(),
                    notification.getTaskId(),
                    notification.getType(),
                    notification.getRecipientId());
        } catch (DuplicateKeyException e) {
            log.info("通知已存在，忽略重复消息: taskId={}, type={}",
                    eventMessage.getTaskId(), eventMessage.getType());
        }
    }
}
