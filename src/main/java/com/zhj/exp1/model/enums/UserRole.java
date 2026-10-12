package com.zhj.exp1.model.enums;

public enum UserRole {
    BUYER("买家"),
    SELLER("卖家"),
    ADMIN("管理员");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}