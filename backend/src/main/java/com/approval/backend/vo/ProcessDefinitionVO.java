package com.approval.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessDefinitionVO {
    private Long id;
    private String name;
    private Integer version;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
