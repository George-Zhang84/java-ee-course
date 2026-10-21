package com.zhj.model.enums;

public enum UserStatus {
    ACTIVE("正常"),
    DISABLED("禁用");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}