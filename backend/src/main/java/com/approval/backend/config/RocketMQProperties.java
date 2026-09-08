package com.approval.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ 配置属性类
 * <p>
 * 通过 {@code @ConfigurationProperties(prefix = "rocketmq")} 自动绑定
 * application.yml 中 {@code rocketmq.*} 前缀的配置项，
 * 由 {@link RocketMQConfig} 中的 {@code @EnableConfigurationProperties} 注册为 Bean。
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "rocketmq")
public class RocketMQProperties {

    /** NameServer 地址 */
    private String nameServer;

    /** 生产者组名 */
    private String producerGroup;

    /** 消费者组名 */
    private String consumerGroup;

    /** 同步发送超时时间（毫秒），默认 3000 */
    private int sendMsgTimeout = 3000;
}
