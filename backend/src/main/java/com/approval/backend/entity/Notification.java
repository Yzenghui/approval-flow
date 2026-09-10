package com.approval.backend.entity;

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
    /** 通知类型：0驳回 1待办 2通过 */
    private Integer type;
    private String content;
    /** 是否已读：0未读 1已读 */
    @TableField("is_read")
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
