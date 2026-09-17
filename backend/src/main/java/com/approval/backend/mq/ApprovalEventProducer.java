package com.approval.backend.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 审批事件生产者：只负责把"已经准备好的消息体"投递到 RocketMQ
 * <p>
 * 序列化与登记在事务内由 {@code MqOutboxService} 完成（写入本地消息表），
 * 何时发送、失败后如何退避、何时放弃，全部由 {@code MqOutboxScheduler} 的扫表逻辑决定，
 * 本类不持有任何投递策略，只做网络调用。
 * </p>
 * <p>
 * 发送失败直接抛出：调用方（扫表任务）会据此走重试/死信状态机，
 * 这里不做任何吞异常的兜底，吞掉即等于丢失。
 * </p>
 */
@Slf4j
@Component
public class ApprovalEventProducer {

    private final DefaultMQProducer producer;

    public ApprovalEventProducer(DefaultMQProducer producer) {
        this.producer = producer;
    }

    /**
     * 同步发送一条消息，等 Broker 确认
     *
     * @param topic   消息主题
     * @param tag     消息标签，可为 null
     * @param msgKey  消息业务键：RocketMQ 按 key 建索引，排查任务通知是否发出时，可在控制台按 key 精确查到
     * @param body    已完成序列化的消息体
     * @throws Exception 网络或 Broker 异常，交由调用方决定重试策略
     */
    public void send(String topic, String tag, String msgKey, String body) throws Exception {
        Message message = new Message(topic, tag, body.getBytes(StandardCharsets.UTF_8));
        message.setKeys(msgKey);

        // 同步发送：拿到 Broker 的确认结果，便于日志留痕与排查
        SendResult sendResult = producer.send(message);
        log.info("审批事件投递成功: topic={}, key={}, sendStatus={}, msgId={}",
                topic,
                msgKey,
                sendResult.getSendStatus(),
                sendResult.getMsgId());
    }
}