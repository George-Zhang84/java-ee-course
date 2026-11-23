package com.zhj.exp4.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 订单支付成功领域事件。
 *
 * <p>为什么用事件而不是在支付方法里直接"发通知 + 加信用分"：
 * <ul>
 *   <li>支付是核心事务，通知属于旁路能力，不该让通知失败回滚支付；</li>
 *   <li>后续新增"统计销售额""发放积分"等动作时，只需加一个监听器，支付代码不动（开闭原则）。</li>
 * </ul>
 *
 * <p>监听器使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}，
 * 保证只有事务真正提交后才执行，避免"通知发出去了但支付回滚了"。
 */
@Getter
public class OrderPaidEvent extends ApplicationEvent {

    private final Long orderId;
    private final String orderNo;
    private final Long buyerId;
    private final Long sellerId;
    private final BigDecimal amount;

    public OrderPaidEvent(Object source, Long orderId, String orderNo,
                          Long buyerId, Long sellerId, BigDecimal amount) {
        super(source);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
    }
}
