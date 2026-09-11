package com.approval.backend.mq;

import com.approval.backend.config.rocketmq.RocketMQConstant;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审批事件生产者单元测试（纯单测，不启动 Spring、不连 RocketMQ）
 */
@DisplayName("审批事件生产者")
class ApprovalEventProducerTest {

    private DefaultMQProducer producer;
    private ObjectMapper objectMapper;
    private ApprovalEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        producer = mock(DefaultMQProducer.class);
        objectMapper = new ObjectMapper();
        eventProducer = new ApprovalEventProducer(producer, objectMapper);
    }

    @Test
    @DisplayName("投递：topic 正确、body 可原样反序列化、key 为 taskId")
    void shouldSendJsonMessageWithTaskIdAsKey() throws Exception {
        when(producer.send(any(Message.class))).thenReturn(sendOk());

        ApprovalEventMessage eventMessage = sampleEvent();
        eventProducer.sendAfterCommit(eventMessage);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        Message sent = captor.getValue();

        assertEquals(RocketMQConstant.APPROVAL_EVENT_TOPIC, sent.getTopic());
        assertEquals("1", sent.getKeys(), "key 用 taskId，供控制台按业务主键反查消息");

        ApprovalEventMessage roundTrip = objectMapper.readValue(
                new String(sent.getBody(), StandardCharsets.UTF_8), ApprovalEventMessage.class);
        assertEquals(eventMessage, roundTrip, "消息体应可无损还原");
    }

    @Test
    @DisplayName("发送失败：只记日志，不向上抛，不影响已提交的审批结果")
    void shouldSwallowSendFailure() throws Exception {
        when(producer.send(any(Message.class))).thenThrow(new RuntimeException("broker down"));

        assertDoesNotThrow(() -> eventProducer.sendAfterCommit(sampleEvent()));
    }

    @Test
    @DisplayName("序列化失败：只记日志，不向上抛，且不发消息")
    void shouldSwallowSerializationFailure() throws Exception {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });
        ApprovalEventProducer brokenProducer = new ApprovalEventProducer(producer, brokenMapper);

        assertDoesNotThrow(() -> brokenProducer.sendAfterCommit(sampleEvent()));
        verifyNoInteractions(producer);
    }

    private SendResult sendOk() {
        SendResult sendResult = new SendResult();
        sendResult.setSendStatus(SendStatus.SEND_OK);
        sendResult.setMsgId("msg-1");
        return sendResult;
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
}
