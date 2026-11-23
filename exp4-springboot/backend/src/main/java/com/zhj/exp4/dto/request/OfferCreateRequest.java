package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起议价请求。
 *
 * <p>价格上限不在注解里写死，而是运行期与商品原价比较——因为上限依赖具体商品。
 */
@Data
public class OfferCreateRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "请填写你的出价")
    @DecimalMin(value = "0.01", message = "出价必须大于 0")
    private BigDecimal offerPrice;

    @Size(max = 255, message = "留言不能超过 255 字")
    private String message;
}
