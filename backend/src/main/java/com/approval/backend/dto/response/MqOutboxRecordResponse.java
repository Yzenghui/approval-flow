package com.approval.backend.dto.response;

import com.approval.backend.common.constant.MqOutboxStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表记录响应
 */
@Data
public class MqOutboxRecordResponse {

    private Long id;

    /** 消息主题 */
    private String topic;

    /** 消息业务键（当前取 taskId），供按业务主键反查 */
    private String msgKey;

    /** 状态：PENDING(0)待发送 SENT(1)已发送 DEAD(2)发送失败（死信），取值见 {@link MqOutboxStatus} */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间（退避到期时间），死信无意义 */
    private LocalDateTime nextRetryTime;

    /** 最近一次发送失败原因 */
    private String errorMsg;

    /** 消息体快照（事务内序列化完成的 JSON 原文） */
    private String body;

    /** 记录登记时间 */
    private LocalDateTime createTime;

    /** 最后一次状态变更时间 */
    private LocalDateTime updateTime;
}
