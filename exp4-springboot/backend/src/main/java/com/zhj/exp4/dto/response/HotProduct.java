package com.zhj.exp4.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 热销商品（柱状图 / 排行榜）。
 */
@Data
public class HotProduct {

    private Long productId;

    private String title;

    /** 成交笔数 */
    private Long soldCount;

    /** 累计成交额 */
    private BigDecimal gmv;
}
