package com.zhj.exp4.enums;

import lombok.Getter;

/**
 * 商品状态枚举。
 *
 * <p>用枚举而非魔法字符串：Service 层比较状态时写 {@code ProductStatus.ON_SALE.name()}，
 * 编译期即可发现拼写错误，且新增状态时 IDE 会提示所有引用点。
 */
@Getter
public enum ProductStatus {

    /** 待审核：卖家刚发布，仅卖家与管理员可见 */
    PENDING("待审核"),
    /** 在售：审核通过，可被搜索、可下单、可议价 */
    ON_SALE("在售"),
    /** 已锁定：有人下单但尚未支付，其他买家不可下单 */
    LOCKED("已锁定"),
    /** 已售出：支付完成 */
    SOLD("已售出"),
    /** 审核驳回：附 audit_remark 说明原因 */
    REJECTED("已驳回"),
    /** 已下架：卖家主动下架 */
    OFF_SHELF("已下架");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    /** 是否允许发起议价：只有在售且允许议价的商品可以 */
    public static boolean canBargain(String status, Integer allowBargain) {
        return ON_SALE.name().equals(status) && allowBargain != null && allowBargain == 1;
    }

    /** 是否可被直接购买 */
    public static boolean canBuy(String status) {
        return ON_SALE.name().equals(status);
    }
}
