package com.callcenter.common.exception;

/**
 * 系统统一错误码。
 */
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码。
     *
     * @return error code
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取默认错误消息。
     *
     * @return default message
     */
    public String getMessage() {
        return message;
    }
}