package com.zhj.exp4.enums;

import lombok.Getter;

/**
 * 议价报价单状态枚举。
 *
 * <p>与订单状态机相互独立又有关联：ACCEPTED 的报价单会生成一笔订单，
 * 若该订单超时被取消，报价单要从 ACCEPTED 回到 EXPIRED（视为作废）。
 */
@Getter
public enum OfferStatus {

    /** 待卖家处理 */
    PENDING("待处理"),
    /** 卖家接受，已生成订单 */
    ACCEPTED("已接受"),
    /** 卖家拒绝，买家可再次出价（受次数限制） */
    REJECTED("已拒绝"),
    /** 买家主动撤回 */
    CANCELLED("已撤回"),
    /** 超时未处理或订单取消导致作废 */
    EXPIRED("已过期");

    private final String description;

    OfferStatus(String description) {
        this.description = description;
    }

    /** 是否处于"等待卖家处理"状态（只有该状态可以接受/拒绝/撤回） */
    public boolean isPending() {
        return this == PENDING;
    }
}
