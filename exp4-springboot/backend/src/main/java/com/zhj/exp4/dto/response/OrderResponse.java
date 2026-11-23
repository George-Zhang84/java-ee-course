package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应。
 *
 * <p>除了数据字段，还带四个"当前用户可执行动作"的布尔标记
 * （{@code canPay/canShip/canConfirm/canCancel}）：
 * 由后端根据"状态 + 当前用户角色"计算，前端只负责渲染按钮，
 * 避免前端自己推断规则导致和后端校验不一致。
 */
@Data
@Builder
public class OrderResponse {

    private Long id;
    private String orderNo;
    private Long productId;
    private String productTitle;
    private String productCover;
    private BigDecimal amount;
    private String origin;
    private String originText;
    private Long offerId;
    private String status;
    private String statusText;
    private Long buyerId;
    private String buyerNickname;
    private Long sellerId;
    private String sellerNickname;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
    private String cancelReason;

    private Boolean canPay;
    private Boolean canShip;
    private Boolean canConfirm;
    private Boolean canCancel;
    private Boolean reviewed;
}
