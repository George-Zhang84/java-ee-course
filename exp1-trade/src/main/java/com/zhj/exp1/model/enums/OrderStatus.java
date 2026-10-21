package com.zhj.exp1.model.enums;

public enum OrderStatus {
    PENDING("待确认"),
    CONFIRMED("已确认"),
    RECEIVED("已收货"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}