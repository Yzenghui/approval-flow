package com.approval.backend.config;

import com.approval.backend.constant.RocketMQConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
     * 消费者：监听审批事件主题，实现"发一条收一条"的链路验证
     */
    @Bean(destroyMethod = "shutdown")
    public DefaultMQPushConsumer defaultMQPushConsumer(RocketMQProperties props) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(props.getConsumerGroup());
        consumer.setNamesrvAddr(props.getNameServer());
        // 订阅审批事件主题，Tag 用 "*" 表示不过滤，收到所有事件类型
        consumer.subscribe(RocketMQConstant.APPROVAL_EVENT_TOPIC, "*");

        // MessageListenerConcurrently：并发消费监听器
        // 返回 CONSUME_SUCCESS 等价于"手动 ack"，告诉 Broker 这条消息处理成功了
        // 如果返回 RECONSUME_LATER，Broker 会按重试策略再次投递
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    log.info("[RocketMQ 消费] topic={}, msgId={}, tags={}, body={}",
                            msg.getTopic(),
                            msg.getMsgId(),
                            msg.getTags(),
                            new String(msg.getBody(), StandardCharsets.UTF_8));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        consumer.start();
        log.info("RocketMQ Consumer 启动成功, 监听 topic={}", RocketMQConstant.APPROVAL_EVENT_TOPIC);
        return consumer;
    }
}
