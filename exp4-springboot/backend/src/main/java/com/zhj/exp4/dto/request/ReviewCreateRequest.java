package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评价请求。
 *
 * <p>评分限定 1~5 星；内容可以为空（只打星），但字数受限。
 */
@Data
public class ReviewCreateRequest {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer score;

    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;
}
