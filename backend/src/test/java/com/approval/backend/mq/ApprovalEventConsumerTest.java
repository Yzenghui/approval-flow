package com.approval.backend.mq;

import com.approval.backend.config.rocketmq.RocketMQConstant;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审批事件消费者单元测试（纯单测）
 */
@DisplayName("审批事件消费者")
class ApprovalEventConsumerTest {

    private NotificationService notificationService;
    private ApprovalEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notificationService = mock(NotificationService.class);
        consumer = new ApprovalEventConsumer(notificationService, objectMapper);
    }

    @Test
    @DisplayName("合法消息：落库成功后 ack")
    void shouldSaveNotificationAndAck() throws Exception {
        ApprovalEventMessage eventMessage = sampleEvent();

        ConsumeConcurrentlyStatus status = consumer.consumeMessage(
                List.of(messageOf(objectMapper.writeValueAsString(eventMessage))), null);

        assertEquals(ConsumeConcurrentlyStatus.CONSUME_SUCCESS, status);
        // ApprovalEventMessage 由 @Data 生成 equals，可直接比对内容
        verify(notificationService).save(eventMessage);
    }

    @Test
    @DisplayName("破损消息：返回 RECONSUME_LATER，交回 Broker 直至进入死信队列，不静默确认")
    void shouldReconsumeWhenBodyIsNotJson() {
        ConsumeConcurrentlyStatus status = consumer.consumeMessage(List.of(messageOf("i-am-not-json")), null);

        assertEquals(ConsumeConcurrentlyStatus.RECONSUME_LATER, status);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("JSON 合法但字段类型错误：同样视为破损消息重投，不静默确认")
    void shouldReconsumeWhenFieldTypeMismatched() {
        String body = "{\"instanceId\":10,\"taskId\":\"not-a-number\"}";

        ConsumeConcurrentlyStatus status = consumer.consumeMessage(List.of(messageOf(body)), null);

        assertEquals(ConsumeConcurrentlyStatus.RECONSUME_LATER, status);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("落库失败：返回 RECONSUME_LATER，通知不丢")
    void shouldReconsumeWhenSaveFails() throws Exception {
        String body = objectMapper.writeValueAsString(sampleEvent());
        doThrow(new RuntimeException("db down")).when(notificationService).save(any());

        ConsumeConcurrentlyStatus status = consumer.consumeMessage(List.of(messageOf(body)), null);

        assertEquals(ConsumeConcurrentlyStatus.RECONSUME_LATER, status);
    }

    @Test
    @DisplayName("批量场景：任意一条失败则整批重投")
    void shouldReconsumeWholeBatchWhenAnyMessageFails() throws Exception {
        ApprovalEventMessage first = sampleEvent();
        ApprovalEventMessage second = sampleEvent();
        second.setTaskId(2L);

        // 第一条成功、第二条失败
        doNothing().doThrow(new RuntimeException("db down")).when(notificationService).save(any());

        ConsumeConcurrentlyStatus status = consumer.consumeMessage(
                List.of(messageOf(objectMapper.writeValueAsString(first)),
                        messageOf(objectMapper.writeValueAsString(second))),
                null);

        // 返回值只能表达"整批"，已成功的第一条也会被重复投递
        assertEquals(ConsumeConcurrentlyStatus.RECONSUME_LATER, status);
        verify(notificationService, times(2)).save(any());
    }

    @Test
    @DisplayName("空批次：直接 ack")
    void shouldAckEmptyBatch() {
        ConsumeConcurrentlyStatus status = consumer.consumeMessage(List.of(), null);

        assertEquals(ConsumeConcurrentlyStatus.CONSUME_SUCCESS, status);
        verifyNoInteractions(notificationService);
    }

    private ApprovalEventMessage sampleEvent() {
        ApprovalEventMessage eventMessage = new ApprovalEventMessage();
        eventMessage.setInstanceId(10L);
        eventMessage.setTaskId(1L);
        eventMessage.setType(2);
        eventMessage.setRecipientId(1L);
        eventMessage.setContent("审批通过");
        return eventMessage;
    }

    private MessageExt messageOf(String body) {
        MessageExt message = new MessageExt();
        message.setTopic(RocketMQConstant.APPROVAL_EVENT_TOPIC);
        message.setMsgId("msg-1");
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        return message;
    }
}
