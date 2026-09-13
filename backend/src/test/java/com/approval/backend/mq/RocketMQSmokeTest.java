package com.approval.backend.mq;

import com.approval.backend.config.rocketmq.RocketMQConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 环境联通冒烟测试（手工探针，不参与 {@code mvn test}）
 * <p>
 * 目的：验证 Windows 后端代码能连上虚拟机里的 RocketMQ，
 * 生产者发一条、消费者收一条。
 * </p>
 * <p>
 * 此处无断言，判定靠人工看日志，并且依赖真实 Broker 在跑、还要额外等待 15 秒，
 * 因此打上 {@code @Tag("manual")}，由 surefire 在 {@code mvn test} 时排除，
 * 需要时在 IDEA 中单独运行即可。
 * </p>
 * <p>
 * 运行方式：在 IDEA 中直接运行本测试方法，观察控制台日志应依次出现两行：
 * </p>
 * <ol>
 *   <li>{@code 测试消息发送成功: SEND_OK} —— 消息已投递到 Broker</li>
 *   <li>{@code 非审批事件消息，已跳过: msgId=..., body=hello-rocketmq}
 *       —— 消费端确实收到了消息，但 body 不是审批事件的 JSON，
 *       按约定跳过并 ack。本测试只验证收发链路连通，不涉及落库</li>
 * </ol>
 */
@Slf4j
@SpringBootTest
@Tag("manual")
class RocketMQSmokeTest {

    @Autowired
    private DefaultMQProducer producer;

    @Test
    void sendAndConsume() throws Exception {
        // 等待 Consumer 完成 Broker 注册与队列 rebalance
        // 新 consumer 组初次就绪可能耗时数秒，若不等就直接发消息，
        // 消费者可能还没分配到队列，导致"发送成功但收不到"的假象
        log.info("等待 Consumer 完成 rebalance...");
        Thread.sleep(5000);

        // 构造消息：topic 指定审批事件主题，tag 用 smoke-test 标记这是一条链路测试消息
        Message message = new Message(
                RocketMQConstant.APPROVAL_EVENT_TOPIC,
                "smoke-test",
                "hello-rocketmq".getBytes(StandardCharsets.UTF_8)
        );

        // 同步发送：send() 会等待 Broker 返回确认结果后才返回
        SendResult sendResult = producer.send(message);
        log.info("测试消息发送成功: {}", sendResult.getSendStatus());

        // 给 Consumer 留足拉取与消费时间，再结束测试进程
        Thread.sleep(10000);
    }
}
