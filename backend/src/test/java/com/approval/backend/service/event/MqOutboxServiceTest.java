package com.approval.backend.service.event;

import com.approval.backend.config.rocketmq.RocketMQConstant;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.entity.MqOutbox;
import com.approval.backend.mapper.MqOutboxMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 本地消息表服务单元测试（纯单测，不启动 Spring、不连数据库）
 * <p>
 * 覆盖写入侧（登记快照）与状态机（已发送 / 退避重试 / 死信）两类行为。
 * 何时被扫到、怎么发给 MQ 属于 {@code MqOutboxScheduler} 的职责，不在本用例范围。
 * </p>
 */
@DisplayName("本地消息表服务")
class MqOutboxServiceTest {

    /** 与 MqOutboxService.MAX_RETRY 对齐，第 10 次失败即转死信 */
    private static final int MAX_RETRY = 10;

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_SENT = 1;
    private static final int STATUS_DEAD = 2;

    private MqOutboxMapper mqOutboxMapper;
    private ObjectMapper objectMapper;
    private MqOutboxService mqOutboxService;

    /**
     * Lambda 条件在 {@code getSqlSegment()} 时才解析列名，而列名来自实体元数据缓存。
     * 纯单测没有 Spring 上下文，需要手动把 MqOutbox 的 TableInfo 注册进去。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MqOutbox.class);
    }

    @BeforeEach
    void setUp() {
        mqOutboxMapper = mock(MqOutboxMapper.class);
        // 事件是简单 POJO（Long / Integer / String），裸 ObjectMapper 足以表达真实的序列化行为
        objectMapper = new ObjectMapper();
        mqOutboxService = new MqOutboxService(mqOutboxMapper, objectMapper);
    }

    @Test
    @DisplayName("登记：以 PENDING 落库，body 为完整快照，msg_key 取 taskId")
    void saveShouldInsertPendingRowWithBodySnapshot() {
        ApprovalEventMessage event = event(100L, 7L, ApprovalEventMessage.TYPE_TODO, 2L, "您有新的待办");

        mqOutboxService.save(event);

        MqOutbox row = captureInserted();
        assertEquals(RocketMQConstant.APPROVAL_EVENT_TOPIC, row.getTopic());
        assertEquals("7", row.getMsgKey(), "msg_key 取 taskId 字符串，供控制台按业务主键反查");
        assertEquals(STATUS_PENDING, row.getStatus());
        assertEquals(0, row.getRetryCount());
        assertNotNull(row.getNextRetryTime());
        assertFalse(row.getNextRetryTime().isAfter(LocalDateTime.now()), "落库即到期，首轮扫表就应该取走");
    }

    @Test
    @DisplayName("登记：body 反序列化后与原事件完全一致")
    void saveShouldStoreLosslessSnapshot() throws Exception {
        ApprovalEventMessage event = event(100L, 7L, ApprovalEventMessage.TYPE_REJECT, 1L, "您的申请已被驳回");

        mqOutboxService.save(event);

        ApprovalEventMessage restored = objectMapper.readValue(captureInserted().getBody(), ApprovalEventMessage.class);
        assertEquals(event, restored);
    }

    @Test
    @DisplayName("登记：序列化失败直接抛出，让业务事务跟着回滚，而不是静默丢消息")
    void saveShouldFailFastWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {
                });
        MqOutboxService failingService = new MqOutboxService(mqOutboxMapper, failingMapper);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> failingService.save(event(100L, 7L, ApprovalEventMessage.TYPE_TODO, 2L, "内容")));

        assertTrue(ex.getMessage().contains("序列化失败"), "异常信息应说明是哪个环节出的问题");
        verify(mqOutboxMapper, never()).insert(any(MqOutbox.class));
    }

    @Test
    @DisplayName("扫表取数：只取 PENDING 且已到期的记录，按 id 升序并受 limit 约束")
    void loadDueShouldQueryOnlyPendingAndDueRecords() {
        MqOutbox due = outbox(1L, 0);

        when(mqOutboxMapper.selectList(any())).thenReturn(List.of(due));

        List<MqOutbox> result = mqOutboxService.loadDue(100);

        assertEquals(List.of(due), result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MqOutbox>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mqOutboxMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();

        assertTrue(sql.contains("status"), "必须先按状态过滤，否则已发送的记录会被反复投递: " + sql);
        assertTrue(sql.contains("next_retry_time"), "退避中的记录不能被提前捞出来: " + sql);
        assertTrue(sql.contains("ORDER BY id ASC"), "按 id 升序保证先登记的先发送: " + sql);
        assertTrue(sql.contains("LIMIT 100"), "单批条数应由调用方传入并落到 SQL: " + sql);
    }

    @Test
    @DisplayName("发送成功：只改状态为已发送")
    void markSentShouldOnlyFlipStatus() {
        mqOutboxService.markSent(42L);

        MqOutbox update = captureUpdate();
        assertEquals(42L, update.getId());
        assertEquals(STATUS_SENT, update.getStatus());
    }

    @Test
    @DisplayName("首次发送失败：重试次数+1，按第一档退避 5 秒，状态保持待发送")
    void markFailedShouldBackOffFiveSecondsOnFirstFailure() {
        MqOutbox record = outbox(1L, 0);

        mqOutboxService.markFailed(record, new RuntimeException("broker down"));

        MqOutbox update = captureUpdate();
        assertEquals(1, update.getRetryCount());
        assertNull(update.getStatus(), "一次失败不等于死信，状态列不应被改写");
        assertEquals("broker down", update.getErrorMsg());
        assertBackoffWithin(update, 5);
    }

    @Test
    @DisplayName("再次失败：退避间隔按档位递增，而非固定 5 秒")
    void markFailedShouldUseLongerBackoffForLaterRetries() {
        MqOutbox secondAttempt = outbox(1L, 1);

        mqOutboxService.markFailed(secondAttempt, new RuntimeException("broker down"));

        MqOutbox update = captureUpdate();
        assertEquals(2, update.getRetryCount());
        assertBackoffWithin(update, 30);
    }

    @Test
    @DisplayName("重试达到上限：转死信，不再安排下次重试")
    void markFailedShouldTurnDeadLetterAtMaxRetry() {
        MqOutbox lastAttempt = outbox(1L, MAX_RETRY - 1);

        mqOutboxService.markFailed(lastAttempt, new RuntimeException("broker down"));

        MqOutbox update = captureUpdate();
        assertEquals(STATUS_DEAD, update.getStatus());
        assertNull(update.getNextRetryTime(), "死信不再被扫描，无需推迟下次重试时间");
    }

    @Test
    @DisplayName("失败原因超长：截断到 error_msg 列宽，不因写库失败再丢一条消息")
    void markFailedShouldTruncateLongError() {
        MqOutbox record = outbox(1L, 0);

        mqOutboxService.markFailed(record, new RuntimeException("x".repeat(600)));

        assertEquals(500, captureUpdate().getErrorMsg().length());
    }

    @Test
    @DisplayName("异常没有 message：退化为类名，error_msg 不留空")
    void markFailedShouldFallbackToExceptionType() {
        MqOutbox record = outbox(1L, 0);

        mqOutboxService.markFailed(record, new RuntimeException());

        assertEquals("RuntimeException", captureUpdate().getErrorMsg());
    }

    private void assertBackoffWithin(MqOutbox update, long expectedSeconds) {
        LocalDateTime earliest = LocalDateTime.now().plusSeconds(expectedSeconds - 1);
        LocalDateTime latest = LocalDateTime.now().plusSeconds(expectedSeconds + 1);
        assertTrue(update.getNextRetryTime().isAfter(earliest) && update.getNextRetryTime().isBefore(latest),
                "退避应为 " + expectedSeconds + " 秒左右，实际: " + update.getNextRetryTime());
    }

    private MqOutbox captureInserted() {
        ArgumentCaptor<MqOutbox> captor = ArgumentCaptor.forClass(MqOutbox.class);
        verify(mqOutboxMapper).insert(captor.capture());
        return captor.getValue();
    }

    private MqOutbox captureUpdate() {
        ArgumentCaptor<MqOutbox> captor = ArgumentCaptor.forClass(MqOutbox.class);
        verify(mqOutboxMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private ApprovalEventMessage event(Long instanceId, Long taskId, int type, Long recipientId, String content) {
        ApprovalEventMessage event = new ApprovalEventMessage();
        event.setInstanceId(instanceId);
        event.setTaskId(taskId);
        event.setType(type);
        event.setRecipientId(recipientId);
        event.setContent(content);
        return event;
    }

    private MqOutbox outbox(Long id, int retryCount) {
        MqOutbox record = new MqOutbox();
        record.setId(id);
        record.setTopic(RocketMQConstant.APPROVAL_EVENT_TOPIC);
        record.setMsgKey(String.valueOf(id));
        record.setBody("{}");
        record.setStatus(STATUS_PENDING);
        record.setRetryCount(retryCount);
        return record;
    }
}
