package com.zhj.exp4.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态机单元测试。
 *
 * <p>状态机是整个交易流程的安全边界：如果允许"已取消"再回到"已支付"，
 * 就会出现重复支付、库存回滚错乱等严重问题。这里把合法/非法流转全部固化。
 */
@DisplayName("订单状态机")
class OrderStatusTest {

    @Test
    @DisplayName("正常流转：待支付 → 已支付 → 已发货 → 已完成")
    void normalFlow() {
        assertTrue(OrderStatus.UNPAID.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("待支付可以取消")
    void unpaidCanCancel() {
        assertTrue(OrderStatus.UNPAID.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("不能越过支付直接发货")
    void cannotSkipPay() {
        assertFalse(OrderStatus.UNPAID.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.UNPAID.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("终态不可再流转（防止重复支付/重复取消）")
    void finalStatesAreTerminal() {
        assertTrue(OrderStatus.COMPLETED.isFinal());
        assertTrue(OrderStatus.CANCELLED.isFinal());
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderStatus.COMPLETED.canTransitionTo(target));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target));
        }
    }

    @Test
    @DisplayName("已支付订单不能直接取消（需要走退款流程，本期未开放）")
    void paidCannotCancel() {
        assertFalse(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("商品状态：只有 ON_SALE 可购买，只有 ON_SALE 且允许议价才能还价")
    void productStatusRules() {
        assertTrue(ProductStatus.canBuy(ProductStatus.ON_SALE.name()));
        assertFalse(ProductStatus.canBuy(ProductStatus.LOCKED.name()));
        assertFalse(ProductStatus.canBuy(ProductStatus.SOLD.name()));

        assertTrue(ProductStatus.canBargain(ProductStatus.ON_SALE.name(), 1));
        assertFalse(ProductStatus.canBargain(ProductStatus.ON_SALE.name(), 0));
        assertFalse(ProductStatus.canBargain(ProductStatus.LOCKED.name(), 1));
        assertFalse(ProductStatus.canBargain(ProductStatus.ON_SALE.name(), null));
    }
}
