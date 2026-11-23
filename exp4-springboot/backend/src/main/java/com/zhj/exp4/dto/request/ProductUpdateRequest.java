package com.zhj.exp4.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 编辑商品请求。
 *
 * <p>字段与发布一致，但只有"卖家本人 + 商品处于可编辑状态（待审核/已驳回/已下架）"才允许修改；
 * 已在售或已锁定的商品必须先下架，避免改价影响进行中的订单。
 */
@Data
@Schema(description = "编辑商品")
public class ProductUpdateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 64, message = "标题不能超过 64 字")
    private String title;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @DecimalMax(value = "100000.00", message = "价格不能超过 10 万")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "底价不能为负")
    private BigDecimal minPrice;

    @Min(value = 0, message = "allowBargain 只能为 0 或 1")
    @Max(value = 1, message = "allowBargain 只能为 0 或 1")
    private Integer allowBargain;

    @Size(max = 16, message = "成色描述过长")
    private String conditionLevel;

    @Size(max = 64, message = "交易地点过长")
    private String location;

    @Size(max = 1000, message = "描述不能超过 1000 字")
    private String description;

    @Size(max = 5, message = "最多上传 5 张图片")
    private List<String> images;
}
