package com.selfinvestor.earnings.controller;

import com.selfinvestor.earnings.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Map;

/**
 * REST 接口统一异常处理器。
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {
    /**
     * 将业务异常转换为 400 响应。
     *
     * @param exception 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBusinessException(BusinessException exception) {
        log.warn("Business request failed: {}", exception.getMessage());
        return Collections.singletonMap("error", exception.getMessage());
    }

    /**
     * 将未预期异常转换为 500 响应。
     *
     * @param exception 未预期异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpectedException(Exception exception) {
        log.error("Unexpected request failure", exception);
        return Collections.singletonMap("error", "Internal server error");
    }
}
