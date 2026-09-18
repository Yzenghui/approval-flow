package com.approval.backend.entity;

import com.approval.backend.common.constant.TaskStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务
 */
@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;
    private Long nodeId;
    private String nodeName;
    private Long assigneeId;
    private Integer submitRound;
    /** 状态：PENDING(0)待处理 COMPLETED(1)已处理 CANCELLED(2)已取消，取值见 {@link TaskStatus} */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
