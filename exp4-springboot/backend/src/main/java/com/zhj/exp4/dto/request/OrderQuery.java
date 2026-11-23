package com.zhj.exp4.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单列表查询参数。
 *
 * <p>{@code role} 决定"我买到的"还是"我卖出的"：
 * buyer 查 buyer_id = 当前用户，seller 查 seller_id = 当前用户。
 * 不传默认 buyer，避免一次查询把"既买又卖"的账号数据混在一起。
 */
@Data
@Schema(description = "订单查询参数")
public class OrderQuery {

    @Schema(description = "视角：buyer 我买到的 / seller 我卖出的")
    private String role = "buyer";

    @Schema(description = "状态筛选：UNPAID/PAID/SHIPPED/COMPLETED/CANCELLED")
    private String status;

    private Integer page = 1;

    private Integer size = 10;

    public boolean isSellerView() {
        return "seller".equalsIgnoreCase(role);
    }

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safeSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }
}
