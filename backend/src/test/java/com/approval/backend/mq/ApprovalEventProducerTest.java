package com.approval.backend.mq;

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
    private ApprovalEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        producer = mock(DefaultMQProducer.class);
        eventProducer = new ApprovalEventProducer(producer);
    }

    @Test
    @DisplayName("投递：topic / key / body 原样透传给 Broker")
    void shouldPassThroughTopicKeyAndBody() throws Exception {
        when(producer.send(any(Message.class))).thenReturn(sendOk());

        String body = "{\"taskId\":1,\"type\":2}";
        eventProducer.send("approval-event", "1", body);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        Message sent = captor.getValue();

        assertEquals("approval-event", sent.getTopic());
        assertEquals("1", sent.getKeys(), "key 用 taskId，供控制台按业务主键反查消息");
        assertEquals(body, new String(sent.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("发送失败：向上抛给扫表任务，由它驱动重试/死信状态机")
    void shouldPropagateSendFailure() throws Exception {
        when(producer.send(any(Message.class))).thenThrow(new RuntimeException("broker down"));

        assertThrows(RuntimeException.class,
                () -> eventProducer.send("approval-event", "1", "{}"));
    }

    private SendResult sendOk() {
        SendResult sendResult = new SendResult();
        sendResult.setSendStatus(SendStatus.SEND_OK);
        sendResult.setMsgId("msg-1");
        return sendResult;
    }
}