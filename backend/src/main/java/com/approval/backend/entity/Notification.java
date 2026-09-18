package com.approval.backend.entity;

import com.approval.backend.common.constant.NotificationReadStatus;
import com.approval.backend.dto.message.ApprovalEventMessage;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知
 */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;
    private Long taskId;
    private Long recipientId;
    /** 通知类型：TYPE_REJECT(0)驳回 TYPE_TODO(1)待办 TYPE_APPROVED(2)通过，取值见 {@link ApprovalEventMessage} */
    private Integer type;
    private String content;
    /** 是否已读：UNREAD(0)未读 READ(1)已读，取值见 {@link NotificationReadStatus} */
    @TableField("is_read")
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
