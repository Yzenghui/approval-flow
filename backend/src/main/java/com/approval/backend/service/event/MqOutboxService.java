package com.approval.backend.service.event;

import com.approval.backend.config.rocketmq.RocketMQConstant;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.MqOutbox;
import com.approval.backend.common.constant.MqOutboxStatus;
import com.approval.backend.mapper.MqOutboxMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表服务
 * <p>
 * 写入侧：业务事务内把 {@link ApprovalEventMessage} 序列化后登记到 mq_outbox，
 * 与业务写库同事务提交/回滚，业务成功即消息已被持久化，投递不再依赖事务提交后的网络调用。
 * </p>
 * <p>
 * 扫描侧：{@code MqOutboxScheduler} 定时取走"到期未发送"的记录，逐条调
 * {@code ApprovalEventProducer} 发送，并按结果驱动状态机：
 * 0 待发送 → 1 已发送；失败则 retry_count+1 并按退避延后 next_retry_time，
 * 超过 {@link #MAX_RETRY} 次转为 2（死信，等人工处理）。
 * </p>
 * <p>
 * 至少一次投递：同一记录可能被重复发出（调度器不保证恰好一次），
 * 由消费端 notification 表的 uk_task_type 唯一键兜底幂等。
 * </p>
 */
@Component
public class MqOutboxService {

    /** 最大重试次数，超出后转死信，不再自动投递 */
    private static final int MAX_RETRY = 10;

    /**
     * 退避间隔（秒），按 retry_count 索引取值，越往后间隔越长；
     * 超出数组长度后固定用最后一档，避免故障期间扫表空转
     */
    private static final long[] BACKOFF_SECONDS = {5, 30, 120, 600, 1800, 3600};

    private final MqOutboxMapper mqOutboxMapper;
    private final ObjectMapper objectMapper;

    public MqOutboxService(MqOutboxMapper mqOutboxMapper, ObjectMapper objectMapper) {
        this.mqOutboxMapper = mqOutboxMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 登记一条待发送消息
     * <p>
     * 必须在业务事务内调用，与业务写库同事务提交。
     * 序列化失败属于代码缺陷，直接抛出让整个业务事务回滚，而非吞掉异常让通知静默丢失。
     * </p>
     */
    public void save(ApprovalEventMessage eventMessage) {
        String body;
        try {
            body = objectMapper.writeValueAsString(eventMessage);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("审批事件序列化失败: " + eventMessage, e);
        }

        MqOutbox outbox = new MqOutbox();
        outbox.setTopic(RocketMQConstant.APPROVAL_EVENT_TOPIC);
        outbox.setMsgKey(String.valueOf(eventMessage.getTaskId()));
        outbox.setBody(body);
        outbox.setStatus(MqOutboxStatus.PENDING);
        outbox.setRetryCount(0);
        // 落库即到期：第一次扫描就会取走
        outbox.setNextRetryTime(LocalDateTime.now());
        mqOutboxMapper.insert(outbox);
    }

    /**
     * 取出到期的待发送记录
     * <p>
     * 单机部署下由 fixedDelay 保证不会重入；
     * 若是多实例部署，需在取记录处引入抢占（如先 update 状态再发送），
     * 重复发送本身由消费端唯一键幂等兜底，不会造成重复通知。
     * </p>
     */
    public List<MqOutbox> loadDue(int limit) {
        return mqOutboxMapper.selectList(new LambdaQueryWrapper<MqOutbox>()
                .eq(MqOutbox::getStatus, MqOutboxStatus.PENDING)
                .le(MqOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(MqOutbox::getId)
                .last("LIMIT " + limit));
    }

    /** 标记发送成功 */
    public void markSent(Long id) {
        MqOutbox update = new MqOutbox();
        update.setId(id);
        update.setStatus(MqOutboxStatus.SENT);
        mqOutboxMapper.updateById(update);
    }

    /**
     * 登记一次发送失败：累加重试次数并按退避推迟下次扫描，超过上限转死信。
     */
    public void markFailed(MqOutbox outbox, Exception e) {
        int nextRetryCount = outbox.getRetryCount() + 1;

        MqOutbox update = new MqOutbox();
        update.setId(outbox.getId());
        update.setRetryCount(nextRetryCount);
        update.setErrorMsg(abbreviate(e));

        if (nextRetryCount >= MAX_RETRY) {
            update.setStatus(MqOutboxStatus.DEAD);
            // 死信不再被扫描，next_retry_time 无意义，保持原值
        } else {
            long backoff = BACKOFF_SECONDS[Math.min(nextRetryCount - 1, BACKOFF_SECONDS.length - 1)];
            update.setNextRetryTime(LocalDateTime.now().plusSeconds(backoff));
        }
        mqOutboxMapper.updateById(update);
    }

    /** 失败原因截断到 error_msg 列宽（500 字符）以内 */
    private String abbreviate(Exception e) {
        String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}