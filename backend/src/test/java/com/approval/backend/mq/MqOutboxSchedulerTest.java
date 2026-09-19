package com.approval.backend.mq;

import com.approval.backend.common.constant.MqOutboxStatus;
import com.approval.backend.entity.MqOutbox;
import com.approval.backend.service.event.MqOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 本地消息表扫表任务单元测试（纯单测，不启动 Spring、不连 RocketMQ）
 * <p>
 * 本类只负责"驱动"：取到期的记录、逐条投递、按结果回写状态。
 * 投递内容是否正确由生产者测试保证，退避与死信的算法由 {@code MqOutboxServiceTest} 保证。
 * </p>
 */
@DisplayName("本地消息表扫表任务")
class MqOutboxSchedulerTest {

    private static final int BATCH_LIMIT = 100;

    private MqOutboxService mqOutboxService;
    private ApprovalEventProducer approvalEventProducer;
    private MqOutboxScheduler scheduler;

    @BeforeEach
    void setUp() {
        mqOutboxService = mock(MqOutboxService.class);
        approvalEventProducer = mock(ApprovalEventProducer.class);
        scheduler = new MqOutboxScheduler(mqOutboxService, approvalEventProducer);
    }

    @Test
    @DisplayName("没有到期待发记录：直接返回，不触碰 MQ")
    void shouldDoNothingWhenNothingDue() {
        when(mqOutboxService.loadDue(anyInt())).thenReturn(List.of());

        scheduler.dispatchPending();

        verifyNoInteractions(approvalEventProducer);
    }

    @Test
    @DisplayName("单批上限：取数上限受 BATCH_LIMIT 约束，避免一轮跑太久")
    void shouldRespectBatchLimit() {
        when(mqOutboxService.loadDue(anyInt())).thenReturn(List.of());

        scheduler.dispatchPending();

        verify(mqOutboxService).loadDue(BATCH_LIMIT);
    }

    @Test
    @DisplayName("投递成功：topic / key / body 原样取自本地消息表，并标记已发送")
    void shouldSendAndMarkSent() throws Exception {
        when(mqOutboxService.loadDue(anyInt())).thenReturn(List.of(
                outbox(1L, "approval-event", "7", "{\"taskId\":7}")));

        scheduler.dispatchPending();

        verify(approvalEventProducer).send(eq("approval-event"), eq("7"), eq("{\"taskId\":7}"));
        verify(mqOutboxService).markSent(1L);
        verify(mqOutboxService, never()).markFailed(any(), any());
    }

    @Test
    @DisplayName("单条失败：该条转失败状态机，同批后续记录照常发送")
    void shouldIsolateFailureWithinBatch() throws Exception {
        MqOutbox broken = outbox(1L, "approval-event", "1", "{}");
        MqOutbox healthy = outbox(2L, "approval-event", "2", "{}");
        when(mqOutboxService.loadDue(anyInt())).thenReturn(List.of(broken, healthy));

        RuntimeException failure = new RuntimeException("broker down");
        doThrow(failure).when(approvalEventProducer).send(eq("approval-event"), eq("1"), any());

        scheduler.dispatchPending();

        verify(mqOutboxService).markFailed(broken, failure);
        verify(mqOutboxService, never()).markSent(1L);
        verify(mqOutboxService).markSent(2L);
    }

    @Test
    @DisplayName("发送成功但回写失败：不得转入失败状态机（避免重复投递并累加 retry），留在 PENDING 等下轮重扫")
    void shouldNotTreatMarkSentFailureAsSendFailure() throws Exception {
        when(mqOutboxService.loadDue(anyInt())).thenReturn(List.of(
                outbox(1L, "approval-event", "1", "{}")));

        doThrow(new RuntimeException("db writeback failed"))
                .when(mqOutboxService).markSent(1L);

        scheduler.dispatchPending();

        verify(approvalEventProducer).send(eq("approval-event"), eq("1"), eq("{}"));
        verify(mqOutboxService, never()).markFailed(any(), any());
    }

    @Test
    @DisplayName("全部失败：每条都进入失败状态机，不会因为第一条异常就中断整批")
    void shouldMarkFailedForEveryRecordWhenBrokerIsDown() throws Exception {
        when(mqOutboxService.loadDue(anyInt())).thenReturn(List.of(
                outbox(1L, "approval-event", "1", "{}"),
                outbox(2L, "approval-event", "2", "{}")));

        doThrow(new RuntimeException("broker down"))
                .when(approvalEventProducer).send(any(), any(), any());

        scheduler.dispatchPending();

        verify(mqOutboxService, times(2)).markFailed(any(), any());
        verify(mqOutboxService, never()).markSent(any());
    }

    private MqOutbox outbox(Long id, String topic, String msgKey, String body) {
        MqOutbox record = new MqOutbox();
        record.setId(id);
        record.setTopic(topic);
        record.setMsgKey(msgKey);
        record.setBody(body);
        record.setStatus(MqOutboxStatus.PENDING);
        record.setRetryCount(0);
        return record;
    }
}
