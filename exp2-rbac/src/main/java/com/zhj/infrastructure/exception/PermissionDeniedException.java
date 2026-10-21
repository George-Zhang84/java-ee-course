package com.zhj.infrastructure.exception;

public class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}