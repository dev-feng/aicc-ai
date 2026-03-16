package com.callcenter.common.exception;

/**
 * 统一业务异常，承载错误码与友好消息。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 使用错误码枚举创建业务异常。
     *
     * @param errorCode error code
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码枚举和自定义消息创建业务异常。
     *
     * @param errorCode error code
     * @param message custom message
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 获取错误码。
     *
     * @return error code
     */
    public int getCode() {
        return code;
    }
}