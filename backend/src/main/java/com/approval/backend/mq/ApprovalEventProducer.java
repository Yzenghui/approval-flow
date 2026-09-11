package com.approval.backend.mq;

import com.approval.backend.config.rocketmq.RocketMQConstant;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;

/**
 * 审批事件生产者
 * <p>
 * 业务代码在事务内调用
 * {@code publishEvent} 登记事件，本类监听该事件，
 * <b>等事务真正提交成功之后</b>才把消息投递到 RocketMQ。
 * </p>
 */
@Slf4j
@Component
public class ApprovalEventProducer {

    private final DefaultMQProducer producer;

    /**
     * 复用 Spring 容器里已经配置好的 ObjectMapper
     * ObjectMapper 在配置完成后是线程安全的，可以作为单例字段共享。
     */
    private final ObjectMapper objectMapper;

    public ApprovalEventProducer(DefaultMQProducer producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    /**
     * 事务提交后投递审批事件
     *
     * @param eventMessage 事务内登记的事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(ApprovalEventMessage eventMessage) {
        try {
            String body = objectMapper.writeValueAsString(eventMessage);
            Message message = new Message(
                    RocketMQConstant.APPROVAL_EVENT_TOPIC,
                    body.getBytes(StandardCharsets.UTF_8));
            // Key 是 RocketMQ 为"按业务主键检索消息"提供的索引：
            // 用 taskId 做 key，排查"这个任务的通知到底发出去没有"时，
            // 可以在控制台按 key 精确查到，而不用翻全量消息
            message.setKeys(String.valueOf(eventMessage.getTaskId()));

            // 同步发送：拿到 Broker 的确认结果，便于日志留痕与排查
            SendResult sendResult = producer.send(message);
            log.info("审批事件投递成功: type={}, taskId={}, sendStatus={}, msgId={}",
                    eventMessage.getType(),
                    eventMessage.getTaskId(),
                    sendResult.getSendStatus(),
                    sendResult.getMsgId());
        } catch (JsonProcessingException e) {
            log.error("审批事件序列化失败: {}", eventMessage, e);
        } catch (Exception e) {
            // 通知属于附属能力，投递失败只记日志，不影响已经提交的审批结果
            log.error("审批事件投递失败: {}", eventMessage, e);
        }
    }
}
