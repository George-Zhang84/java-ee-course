package com.zhj.exp4.enums;

import lombok.Getter;

/**
 * 钱包流水类型。
 *
 * <p>用枚举 + 描述，避免前端直接显示 RECHARGE 这类英文码；
 * 统计口径也能从这里统一（收入 = INCOME + RECHARGE，支出 = PAY）。
 */
@Getter
public enum WalletType {

    RECHARGE("充值"),
    PAY("支付"),
    INCOME("卖出收入"),
    REFUND("退款");

    private final String description;

    WalletType(String description) {
        this.description = description;
    }

    /** 是否计入收入 */
    public boolean isIncome() {
        return this == RECHARGE || this == INCOME || this == REFUND;
    }
}
