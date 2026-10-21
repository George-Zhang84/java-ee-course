package com.zhj.exp1.exception;

/**
 * 余额不足异常
 */
public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}