package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品审核请求。
 *
 * <p>{@code pass=true} 通过（PENDING→ON_SALE），{@code false} 驳回（PENDING→REJECTED）。
 * 驳回时 {@code remark} 必填，由服务端二次校验，方便卖家知道该改什么。
 */
@Data
public class ProductAuditRequest {

    @NotNull(message = "请选择审核结果")
    private Boolean pass;

    @Size(max = 255, message = "审核意见不能超过 255 字")
    private String remark;
}
