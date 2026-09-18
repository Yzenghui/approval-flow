package com.approval.backend.dto.response;

import com.approval.backend.common.constant.NotificationReadStatus;
import com.approval.backend.dto.message.ApprovalEventMessage;
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
    /** 通知类型：TYPE_REJECT(0)驳回 TYPE_TODO(1)待办 TYPE_APPROVED(2)通过，取值见 {@link ApprovalEventMessage} */
    private Integer type;
    private String content;
    /** 是否已读：UNREAD(0)未读 READ(1)已读，取值见 {@link NotificationReadStatus} */
    private Integer isRead;
    private LocalDateTime createTime;
}
