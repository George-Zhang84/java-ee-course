package com.zhj.exp4.task;

import com.zhj.exp4.service.BargainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 议价报价单过期任务。
 *
 * <p>与订单超时任务同构：定时器只做遍历，单条过期在自己的事务里完成。
 * 报价单过期后买卖双方各收到一条通知，用户可以重新出价（受次数上限约束）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferExpireTask {

    private static final int BATCH_SIZE = 50;

    private final BargainService bargainService;

    @Scheduled(initialDelayString = "${exp4.offer.expire-scan-ms:60000}",
            fixedDelayString = "${exp4.offer.expire-scan-ms:60000}")
    public void expireOffers() {
        try {
            List<Long> offerIds = bargainService.findExpiredIds(BATCH_SIZE);
            if (offerIds.isEmpty()) {
                return;
            }
            int success = 0;
            for (Long offerId : offerIds) {
                try {
                    bargainService.expireOne(offerId);
                    success++;
                } catch (Exception e) {
                    log.warn("报价单过期处理失败 offerId={} 原因={}", offerId, e.getMessage());
                }
            }
            log.info("报价单过期扫描完成：待处理 {} 条，成功 {} 条", offerIds.size(), success);
        } catch (Exception e) {
            log.error("报价单过期任务异常", e);
        }
    }
}
