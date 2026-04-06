package com.approval.backend.common;

import lombok.Data;

/**
 * 统一响应结果封装类
 *
 * @param <T> 响应中数据载荷的类型
 */
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * 成功响应
     *
     * @param data 要返回的业务数据
     * @param <T>  业务数据类型
     * @return 包装了成功状态和数据的响应结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 失败响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     业务数据类型
     * @return 封装了失败状态和错误信息的响应结果
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

}
