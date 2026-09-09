package com.selfinvestor.earnings.common.exception;

/**
 * 可安全返回给调用方的业务异常。
 */
public class BusinessException extends RuntimeException {
    /**
     * 创建业务异常。
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 创建带原始原因的业务异常。
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
