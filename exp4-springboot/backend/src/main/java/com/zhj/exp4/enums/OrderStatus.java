package com.zhj.exp4.enums;

import lombok.Getter;

import java.util.Set;

/**
 * 订单状态枚举 + 合法流转表。
 *
 * <p>把"哪些状态可以流转到哪些状态"声明为数据（{@code ALLOWED})，
 * 由 {@link #canTransitionTo(OrderStatus)} 统一判断，
 * 避免在各个 Service 方法里散落 {@code if (status == ...)} 判断导致漏洞。
 */
@Getter
public enum OrderStatus {

    /** 待支付：下单成功，占用商品 15 分钟 */
    UNPAID("待支付"),
    /** 已支付：钱已从买家钱包扣到卖家钱包（平台内转账，无第三方支付） */
    PAID("已支付"),
    /** 已发货 */
    SHIPPED("已发货"),
    /** 已完成：买家确认收货，可评价 */
    COMPLETED("已完成"),
    /** 已取消：买家主动取消或超时未支付 */
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    /**
     * 合法流转：当前状态 -> 允许到达的状态集合。
     * 终态 COMPLETED 与 CANCELLED 不允许再流转。
     */
    private static Set<OrderStatus> allowedTargets(OrderStatus from) {
        return switch (from) {
            case UNPAID -> Set.of(PAID, CANCELLED);
            case PAID -> Set.of(SHIPPED);
            case SHIPPED -> Set.of(COMPLETED);
            case COMPLETED, CANCELLED -> Set.of();
        };
    }

    /** 判断能否从当前状态流转到目标状态 */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTargets(this).contains(target);
    }

    /** 是否为终态 */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
