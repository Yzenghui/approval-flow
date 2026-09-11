package com.approval.backend.config.rocketmq;

import com.approval.backend.mq.ApprovalEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 * <p>
 * 负责创建并管理 Producer 与 Consumer 的生命周期：
 * </p>
 * <ul>
 *   <li>{@code destroyMethod = "shutdown"}：Spring 容器销毁时自动调用 RocketMQ
 *       自身的 shutdown()，优雅释放连接资源（Broker 会感知下线，避免消息发给已失效的客户端）</li>
 *   <li>Bean 方法上抛出的 {@link MQClientException} 会让应用启动失败</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RocketMQProperties.class)
public class RocketMQConfig {

    /**
     * 生产者：用于业务代码中发送审批事件消息
     */
    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer defaultMQProducer(RocketMQProperties props) throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer(props.getProducerGroup());
        producer.setNamesrvAddr(props.getNameServer());
        // 同步发送超时上限，避免 NameServer/Broker 无响应时线程长时间阻塞
        producer.setSendMsgTimeout(props.getSendMsgTimeout());
        // 同步发送失败自动重试 2 次
        producer.setRetryTimesWhenSendFailed(2);
        // start() 会连接 NameServer，失败将抛 MQClientException 导致启动失败
        producer.start();
        log.info("RocketMQ Producer 启动成功, nameServer={}, group={}",
                props.getNameServer(), props.getProducerGroup());
        return producer;
    }

    /**
     * 消费者：监听审批事件主题，把审批事件落库为通知
     * <p>
     * 本方法只负责消费者的创建、订阅与生命周期管理；
     * 真正的消费逻辑（解析消息、写通知表、幂等处理）在
     * {@link ApprovalEventConsumer} 中，便于单独阅读与测试。
     * </p>
     */
    @Bean(destroyMethod = "shutdown")
    public DefaultMQPushConsumer defaultMQPushConsumer(RocketMQProperties props,
                                                       ApprovalEventConsumer approvalEventConsumer) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(props.getConsumerGroup());
        consumer.setNamesrvAddr(props.getNameServer());
        // 订阅审批事件主题，Tag 用 "*" 表示不过滤，收到所有事件类型
        consumer.subscribe(RocketMQConstant.APPROVAL_EVENT_TOPIC, "*");
        consumer.registerMessageListener(approvalEventConsumer);

        consumer.setConsumeMessageBatchMaxSize(props.getConsumeMessageBatchMaxSize());
        consumer.setMaxReconsumeTimes(props.getMaxReconsumeTimes());

        consumer.start();
        log.info("RocketMQ Consumer 启动成功, 监听 topic={}", RocketMQConstant.APPROVAL_EVENT_TOPIC);
        return consumer;
    }
}
