package com.approval.backend.mq;

import com.approval.backend.dto.message.ApprovalEventMessage;
import com.approval.backend.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 审批事件消费者
 * <p>
 * 由 {@code RocketMQConfig} 注册到 DefaultMQPushConsumer 上，
 * 收到消息后解析并交给 {@link NotificationService} 落库。
 * </p>
 * <p>
 * 返回值语义：
 * </p>
 * <ul>
 *   <li>{@code CONSUME_SUCCESS}：相当于手动 ack，Broker 认为该消息处理完毕</li>
 *   <li>{@code RECONSUME_LATER}：Broker 稍后按重试策略再次投递</li>
 * </ul>
 * <p>
 * 只有"确实处理完了"才返回 ack，解析失败（破损消息）和落库失败一样返回重试。
 * </p>
 */
@Slf4j
@Component
public class ApprovalEventConsumer implements MessageListenerConcurrently {

    private final NotificationService notificationService;

    /**
     * 复用 Spring 容器中已配置好的 ObjectMapper。
     * 消费端是多线程的（默认 20~64 个消费线程），
     * 这里能安全共享，靠的是 ObjectMapper 本身的线程安全性。
     */
    private final ObjectMapper objectMapper;

    public ApprovalEventConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages, ConsumeConcurrentlyContext context) {
        for (MessageExt message : messages) {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            try {
                ApprovalEventMessage eventMessage = objectMapper.readValue(body, ApprovalEventMessage.class);
                notificationService.save(eventMessage);
            } catch (JsonProcessingException e) {
                // 破损消息不能 ack，一旦确认，这条消息就会从系统里永久消失。
                // 应该交回 Broker 重试，超过最大重试次数后进入 %DLQ% 消费者组
                log.error("审批事件解析失败，交回 Broker 重试: msgId={}, body={}", message.getMsgId(), body, e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            } catch (Exception e) {
                // 落库失败（例如数据库暂时不可用）属于可恢复问题，
                // 返回重试让 Broker 稍后再投一次，而不是把通知悄悄丢掉
                log.error("通知落库失败，稍后重试: msgId={}, body={}", message.getMsgId(), body, e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
