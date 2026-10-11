package com.zhj.exp1.exception;

/**
 * 非法操作异常
 * 例如：非法状态跳转、越权操作、重复操作等
 */
public class InvalidOperationException extends BusinessException {

    public InvalidOperationException(String message) {
        super(message);
    }
}