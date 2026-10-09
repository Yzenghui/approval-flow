package com.approval.backend.mq;

import com.approval.backend.entity.MqOutbox;
import com.approval.backend.service.event.MqOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地消息表扫表补发任务
 * <p>
 * 业务事务提交只是把消息"登记"进了 mq_outbox，真正到 RocketMQ 的投递由本任务完成：
 * 每次取一批到期待发记录，逐条同步发送，成功标记已发送，失败交给退避重试状态机。
 * </p>
 * <p>
 * 一次投递失败不等于消息丢失，记录仍在表里，next_retry_time 到期后会被再次扫到，
 * 直到达到最大重试次数转死信为止。
 * </p>
 * <p>
 * fixedDelay 而非 fixedRate：上一轮没处理完（Broker 抖动、大批量积压）就顺延，
 * 避免任务重入导致同一记录被两个执行流同时发送。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mq.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class MqOutboxScheduler {

    /** 单批最多取多少条，防止积压时一轮任务跑太久 */
    private static final int BATCH_LIMIT = 500;

    private final MqOutboxService mqOutboxService;
    private final ApprovalEventProducer approvalEventProducer;

    public MqOutboxScheduler(MqOutboxService mqOutboxService, ApprovalEventProducer approvalEventProducer) {
        this.mqOutboxService = mqOutboxService;
        this.approvalEventProducer = approvalEventProducer;
    }

    /**
     * 扫表间隔默认 5 秒，可用 mq.outbox.scan-interval-ms 调整。
     */
    @Scheduled(fixedDelayString = "${mq.outbox.scan-interval-ms:5000}")
    public void dispatchPending() {
        List<MqOutbox> dueList = mqOutboxService.loadDue(BATCH_LIMIT);
        if (dueList.isEmpty()) {
            return;
        }
        log.debug("本地消息表待发送: {} 条", dueList.size());

        for (MqOutbox outbox : dueList) {
            try {
                approvalEventProducer.send(outbox.getTopic(), outbox.getMsgKey(), outbox.getBody());
            } catch (Exception e) {
                // 单条发送失败不中断整批，进入退避重试状态机
                log.warn("本地消息发送失败: id={}, msgKey={}, retry={}",
                        outbox.getId(), outbox.getMsgKey(), outbox.getRetryCount() + 1, e);
                mqOutboxService.markFailed(outbox, e);
                continue;
            }
            try {
                mqOutboxService.markSent(outbox.getId());
            } catch (Exception e) {
                // 消息实际已到 Broker，不能走失败状态机，否则会被当作没发出去而重复投递并累加 retry。
                // 留在 PENDING，下一轮重扫会重发一次，用日志暴露 DB 回写异常。
                log.error("消息已发送但标记回写失败，下轮将重发: id={}, msgKey={}",
                        outbox.getId(), outbox.getMsgKey(), e);
            }
        }
    }
}