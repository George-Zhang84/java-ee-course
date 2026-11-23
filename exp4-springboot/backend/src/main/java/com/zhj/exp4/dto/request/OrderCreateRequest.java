package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 下单请求（直接购买）。议价成单由"接受报价"接口完成，不走这里。
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;
}
