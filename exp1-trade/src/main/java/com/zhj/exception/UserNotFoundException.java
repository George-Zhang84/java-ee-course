package com.zhj.exception;

/**
 * 用户不存在异常
 */
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String message) {
        super(message);
    }
}