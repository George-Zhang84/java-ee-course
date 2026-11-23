package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 钱包充值请求（演示用模拟充值，无第三方支付）。
 */
@Data
public class RechargeRequest {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    @DecimalMax(value = "10000.00", message = "单次充值不能超过 1 万元")
    private BigDecimal amount;
}
