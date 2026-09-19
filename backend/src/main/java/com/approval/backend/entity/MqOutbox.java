package com.approval.backend.entity;

import com.approval.backend.common.constant.MqOutboxStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息
 * <p>
 * 与业务数据同事务落库，由定时任务异步扫描补发。
 * 作为消息可靠投递的最终兜底，确保业务提交即消息必达。
 * </p>
 */
@Data
@TableName("mq_outbox")
public class MqOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息主题 */
    private String topic;
    /** 消息业务键（当前取 taskId），供控制台按业务主键反查 */
    private String msgKey;
    /** 消息体快照：事务内序列化完成的 JSON，不接受扫表时回查 */
    private String body;
    /** 状态：PENDING(0)待发送 SENT(1)已发送 DEAD(2)发送失败（死信），取值见 {@link MqOutboxStatus} */
    private Integer status;
    /** 已重试次数 */
    private Integer retryCount;
    /** 下次重试时间，未到时间不被扫描（退避重试） */
    private LocalDateTime nextRetryTime;
    /** 最近一次发送失败原因 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
