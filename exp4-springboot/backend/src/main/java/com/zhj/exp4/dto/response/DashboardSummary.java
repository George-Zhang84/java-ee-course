package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据看板汇总指标（顶部卡片）。
 */
@Data
@Builder
public class DashboardSummary {

    /** 用户总数 */
    private Long userCount;
    /** 在售商品数 */
    private Long onSaleCount;
    /** 待审核商品数 */
    private Long pendingCount;
    /** 订单总数 */
    private Long orderCount;
    /** 累计成交额（GMV）：已支付/已发货/已完成订单金额之和 */
    private BigDecimal gmv;
    /** 今日订单数 */
    private Long todayOrders;
    /** 今日成交额 */
    private BigDecimal todayGmv;
}
