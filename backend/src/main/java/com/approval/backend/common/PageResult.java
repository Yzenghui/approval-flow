package com.approval.backend.common;

import lombok.Data;
import java.util.List;

/**
 * 分页结果
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> {
    private Long total;
    private List<T> records;
}