package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.common.constant.NotificationReadStatus;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.dto.response.NotificationResponse;
import com.approval.backend.entity.Notification;
import com.approval.backend.mapper.NotificationMapper;
import com.approval.backend.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        notification.setIsRead(NotificationReadStatus.UNREAD);

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

    /**
     * 分页查询当前用户的通知
     * <p>
     * 收件人条件直接写进 SQL，不依赖上层传参正确；
     * 排序用 create_time + id 双字段，理由见方法内注释。
     * </p>
     */
    @Override
    public PageResult<NotificationResponse> getNotifications(Integer pageNum, Integer pageSize, Integer type, Boolean onlyUnread, Long userId) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientId, userId)
                .eq(type != null, Notification::getType, type)
                .eq(Boolean.TRUE.equals(onlyUnread), Notification::getIsRead, NotificationReadStatus.UNREAD)
                // create_time 是秒精度，同一秒落库的多条通知时间完全相同，
                // 仅按它排序时顺序不稳定，翻页会出现漏记录或重复记录，故用 id 兜底
                .orderByDesc(Notification::getCreateTime)
                .orderByDesc(Notification::getId);
        Page<Notification> notificationPage = notificationMapper.selectPage(page, queryWrapper);

        PageResult<NotificationResponse> pageResult = new PageResult<>();
        pageResult.setTotal(notificationPage.getTotal());
        pageResult.setRecords(notificationPage.getRecords().stream()
                .map(NotificationServiceImpl::toResponse)
                .toList());

        return pageResult;
    }

    /**
     * 统计当前用户未读通知数
     */
    @Override
    public Long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getIsRead, NotificationReadStatus.UNREAD));
    }

    /**
     * 标记单条通知为已读
     * <p>
     * 已读的重复标记幂等放行，不属于当前用户的通知必须报错。
     * 更新本身天然幂等（重复执行最终都是 is_read = 1），并发标记无需加锁。
     * </p>
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID（即收件人）
     */
    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        // 不存在与不属于当前用户返回同一句话，避免用错误信息探测他人通知是否存在
        if (notification == null || !userId.equals(notification.getRecipientId())) {
            throw new BusinessException("通知不存在");
        }
        if (Integer.valueOf(NotificationReadStatus.READ).equals(notification.getIsRead())) {
            return;
        }

        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getRecipientId, userId)
                .set(Notification::getIsRead, NotificationReadStatus.READ));
    }

    /**
     * 标记当前用户全部通知为已读
     * <p>
     * 只带 is_read = 0 条件，避免把已读记录重复更新一遍。
     * </p>
     */
    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getIsRead, NotificationReadStatus.UNREAD)
                .set(Notification::getIsRead, NotificationReadStatus.READ));
    }

    /**
     * 实体转响应对象
     */
    private static NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setInstanceId(notification.getInstanceId());
        response.setTaskId(notification.getTaskId());
        response.setType(notification.getType());
        response.setContent(notification.getContent());
        response.setIsRead(notification.getIsRead());
        response.setCreateTime(notification.getCreateTime());
        return response;
    }
}
