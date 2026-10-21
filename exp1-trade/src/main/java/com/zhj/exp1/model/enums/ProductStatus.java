package com.zhj.exp1.model.enums;

public enum ProductStatus {
    PENDING("待审核"),
    ON_SALE("在售"),
    OFF_SHELF("已下架"),
    SOLD("已售出"),
    REJECTED("已驳回");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}