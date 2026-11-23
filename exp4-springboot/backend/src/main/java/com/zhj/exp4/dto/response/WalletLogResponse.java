package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水响应。typeText 直接把 RECHARGE 翻译成"充值"，前端无需维护映射表。
 */
@Data
@Builder
public class WalletLogResponse {

    private Long id;
    private String type;
    private String typeText;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String bizType;
    private Long bizId;
    private String remark;
    private LocalDateTime createTime;
}
