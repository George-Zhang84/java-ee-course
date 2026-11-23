package com.zhj.exp4.listener;

import com.zhj.exp4.config.AsyncConfig;
import com.zhj.exp4.event.OrderPaidEvent;
import com.zhj.exp4.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单支付成功监听器。
 *
 * <p>两个注解缺一不可：
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)}：只在支付事务提交成功后才执行，
 *       避免出现"通知已发出但支付回滚"的不一致；</li>
 *   <li>{@code @Async}：放到独立线程池，不阻塞支付接口响应。</li>
 * </ul>
 *
 * <p>这就是"观察者模式 + 事件驱动"在本项目中的实际用途：
 * 支付核心逻辑只管扣款与状态，通知这类旁路动作由监听器承担，新增需求只需新增监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidListener {

    private final NotificationService notificationService;

    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        try {
            notificationService.send(event.getSellerId(), "ORDER", "订单已支付",
                    "订单 " + event.getOrderNo() + " 买家已付款 " + event.getAmount()
                            + " 元，请尽快发货", event.getOrderId());
            notificationService.send(event.getBuyerId(), "ORDER", "支付成功",
                    "订单 " + event.getOrderNo() + " 已支付，等待卖家发货", event.getOrderId());
            log.info("订单支付事件已处理 orderNo={}", event.getOrderNo());
        } catch (Exception e) {
            log.warn("处理订单支付事件失败 orderNo={}", event.getOrderNo(), e);
        }
    }
}
