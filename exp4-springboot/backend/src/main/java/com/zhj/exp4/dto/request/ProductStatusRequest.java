package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商品上下架请求。
 *
 * <p>只允许 ON_SALE（上架）与 OFF_SHELF（下架）两个目标状态，
 * 其余状态（SOLD/LOCKED 等）由系统根据交易流程自行流转，不接受外部指定。
 */
@Data
public class ProductStatusRequest {

    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "ON_SALE|OFF_SHELF", message = "目标状态只能是在售或已下架")
    private String status;
}
