package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 发起还价的结果。
 *
 * <p>两种结局都要让前端知道：
 * <ul>
 *   <li>{@code autoAccepted=false}：正常待卖家处理，前端提示"等待卖家回复"；</li>
 *   <li>{@code autoAccepted=true}：出价已达到卖家设置的底价，系统直接成交并生成订单，
 *       前端应引导买家去支付（{@code orderId} 即新订单）。</li>
 * </ul>
 */
@Data
@Builder
public class OfferCreateResponse {

    private Long offerId;

    /** 是否因达到底价而自动成交 */
    private Boolean autoAccepted;

    /** 自动成交生成的订单 id；未成交时为 null */
    private Long orderId;

    /** 给用户看的提示文案 */
    private String message;
}
