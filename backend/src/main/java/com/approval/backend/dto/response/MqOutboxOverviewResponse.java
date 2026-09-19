package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表概览响应
 * <p>
 * 死信大于 0 说明有消息重试耗尽、需要人工介入；
 * 退避中大于 0 说明最近有发送失败正在等下一轮；
 * 积压时间说明待发送队列是不是卡住了。
 * </p>
 */
@Data
public class MqOutboxOverviewResponse {

    /** 待发送总数（含正在退避等待的） */
    private Long pending;

    /** 待发送中处于退避等待、尚未到下次重试时间的条数 */
    private Long waitingBackoff;

    /** 已发送总数 */
    private Long sent;

    /** 死信总数，大于 0 需要人工介入 */
    private Long dead;

    /** 积压最久的一条待发送记录的登记时间；没有待发送记录时为 null */
    private LocalDateTime oldestPendingTime;
}
