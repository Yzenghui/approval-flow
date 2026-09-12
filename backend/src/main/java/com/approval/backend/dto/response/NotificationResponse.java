package com.approval.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知列表项响应数据传输对象
 * <p>
 * instanceId 与 taskId 都要返回给前端：前者用于跳转申请详情，
 * 后者用于待办类通知跳转任务处理，前端不能靠 content 反推。
 * </p>
 */
@Data
public class NotificationResponse {
    private Long id;
    /** 关联流程实例ID */
    private Long instanceId;
    /** 触发该通知的任务ID */
    private Long taskId;
    /** 通知类型：0驳回 1待办 2通过 */
    private Integer type;
    private String content;
    /** 是否已读：0未读 1已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
