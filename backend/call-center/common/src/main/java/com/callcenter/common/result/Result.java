package com.callcenter.common.result;

import com.callcenter.common.exception.ErrorCode;

/**
 * 统一接口响应模型。
 *
 * @param <T> payload type
 */
public class Result<T> {

    private final int code;
    private final String msg;
    private final T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data response payload
     * @param <T> payload type
     * @return success result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 创建失败响应。
     *
     * @param code error code
     * @param msg error message
     * @param <T> payload type
     * @return failed result
     */
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 获取响应码。
     *
     * @return response code
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取响应消息。
     *
     * @return response message
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 获取业务数据。
     *
     * @return response data
     */
    public T getData() {
        return data;
    }
}