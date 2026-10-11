package com.zhj.exp1.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ID生成器
 * 生成规则：前缀 + 4位序号，例如 U0001、P0023、O0008、R0005
 */
public class IdGenerator {

    // AtomicInteger 是线程安全的计数器，比普通 int++ 更可靠
    private static final AtomicInteger userCounter = new AtomicInteger(0);
    private static final AtomicInteger productCounter = new AtomicInteger(0);
    private static final AtomicInteger orderCounter = new AtomicInteger(0);
    private static final AtomicInteger reviewCounter = new AtomicInteger(0);

    /**
     * 生成用户ID，如 U0001
     */
    public static String nextUserId() {
        return String.format("U%04d", userCounter.incrementAndGet());
    }

    /**
     * 生成商品ID，如 P0001
     */
    public static String nextProductId() {
        return String.format("P%04d", productCounter.incrementAndGet());
    }

    /**
     * 生成订单ID，如 O0001
     */
    public static String nextOrderId() {
        return String.format("O%04d", orderCounter.incrementAndGet());
    }

    /**
     * 生成评价ID，如 R0001
     */
    public static String nextReviewId() {
        return String.format("R%04d", reviewCounter.incrementAndGet());
    }
}