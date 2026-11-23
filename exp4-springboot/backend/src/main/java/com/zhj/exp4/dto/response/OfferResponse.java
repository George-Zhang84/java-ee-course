package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 议价报价单响应（买家看"我发出的"、卖家看"我收到的"共用）。
 */
@Data
@Builder
public class OfferResponse {

    private Long id;
    private Long productId;
    private String productTitle;
    private String productCover;
    /** 商品原价，用于前端展示"原价 200 / 出价 180" */
    private BigDecimal productPrice;
    private BigDecimal offerPrice;
    private String message;
    private String status;
    private String statusText;
    private Long buyerId;
    private String buyerNickname;
    private Long sellerId;
    private String sellerNickname;
    private LocalDateTime expireTime;
    private LocalDateTime handledTime;
    private LocalDateTime createTime;

    /** 当前用户是否可接受/拒绝（卖家视角且仍待处理） */
    private Boolean canHandle;
    /** 当前用户是否可撤回（买家视角且仍待处理） */
    private Boolean canCancel;
}
