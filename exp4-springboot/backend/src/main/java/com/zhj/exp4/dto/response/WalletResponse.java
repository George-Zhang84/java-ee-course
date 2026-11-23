package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 钱包概览。统计值由 SQL 聚合得出，避免前端累加全部流水。
 */
@Data
@Builder
public class WalletResponse {

    private BigDecimal balance;
    /** 累计收入（充值 + 卖出 + 退款） */
    private BigDecimal totalIncome;
    /** 累计支出（支付） */
    private BigDecimal totalExpense;
}
