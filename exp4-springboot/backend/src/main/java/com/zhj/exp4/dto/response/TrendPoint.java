package com.zhj.exp4.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 趋势图数据点（近 N 日订单量与成交额）。
 *
 * <p>用 {@code @Data} 而非 {@code @Builder}：MyBatis 通过 setter/字段映射结果集，
 * 需要有默认构造器与 setter。
 */
@Data
public class TrendPoint {

    /** 统计日期 */
    private LocalDate statDate;

    /** 当日订单数 */
    private Long orderCount;

    /** 当日成交额 */
    private BigDecimal gmv;
}
