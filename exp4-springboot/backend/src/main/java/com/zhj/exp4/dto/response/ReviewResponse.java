package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价响应。商品详情页展示的是"买家评卖家"（reviewerRole=BUYER）。
 */
@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long orderId;
    private Long productId;
    private Integer score;
    private String content;
    private String reviewerRole;
    private Long reviewerId;
    private String reviewerNickname;
    private Long revieweeId;
    private LocalDateTime createTime;
}
