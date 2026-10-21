package com.zhj.exp2.infrastructure.exception;

public class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}