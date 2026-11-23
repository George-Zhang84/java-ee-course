package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项。
 *
 * <p>刻意不含 {@code minPrice}（卖家底价）与 {@code auditRemark}：
 * 列表接口对外公开，泄露底价会让买家总能压到最低价。
 */
@Data
@Builder
public class ProductListResponse {

    private Long id;
    private String title;
    private BigDecimal price;
    private String coverUrl;
    private String conditionLevel;
    private String location;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerNickname;
    private Integer allowBargain;
    private String status;
    private String statusText;
    private Integer views;
    private LocalDateTime createTime;
}
