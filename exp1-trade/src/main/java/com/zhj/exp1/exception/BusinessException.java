package com.zhj.exp1.exception;

/**
 * 业务异常基类
 * 所有自定义业务异常都继承它，方便统一捕获
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}