package com.zhj.exp4.task;

import com.zhj.exp4.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时自动取消任务（特色功能 S2）。
 *
 * <p>调度与业务分离：
 * <ul>
 *   <li>本类只负责"定时触发 + 遍历"，不开启事务；</li>
 *   <li>每笔订单的取消交给 {@link OrderService#cancelBySystem(Long)}（独立事务），
 *       这样某笔订单处理失败不会影响其他订单，也不会形成长事务锁表。</li>
 * </ul>
 *
 * <p>扫描间隔默认 60 秒，可通过 {@code exp4.order.timeout-scan-ms} 调整；
 * 单次最多处理 50 笔，防止积压时一次性锁太多行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private static final int BATCH_SIZE = 50;

    private final OrderService orderService;

    @Scheduled(initialDelayString = "${exp4.order.timeout-scan-ms:60000}",
            fixedDelayString = "${exp4.order.timeout-scan-ms:60000}")
    public void cancelTimeoutOrders() {
        try {
            List<Long> orderIds = orderService.findTimeoutOrderIds(BATCH_SIZE);
            if (orderIds.isEmpty()) {
                return;
            }
            int success = 0;
            for (Long orderId : orderIds) {
                try {
                    orderService.cancelBySystem(orderId);
                    success++;
                } catch (Exception e) {
                    // 单笔失败（如刚好被支付）只记录，不中断本轮
                    log.warn("取消超时订单失败 orderId={} 原因={}", orderId, e.getMessage());
                }
            }
            log.info("超时订单扫描完成：待处理 {} 笔，成功取消 {} 笔", orderIds.size(), success);
        } catch (Exception e) {
            log.error("超时订单扫描任务异常", e);
        }
    }
}
