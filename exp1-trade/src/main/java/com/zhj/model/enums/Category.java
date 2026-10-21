package com.zhj.model.enums;

public enum Category {
    BOOK("书籍"),
    ELECTRONICS("电子"),
    DAILY("日用"),
    CLOTHING("服饰"),
    SPORTS("运动"),
    OTHER("其他");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}