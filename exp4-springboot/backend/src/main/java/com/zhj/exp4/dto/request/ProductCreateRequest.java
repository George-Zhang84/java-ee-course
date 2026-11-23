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
 * 发布商品请求。
 *
 * <p>校验规则对应业务约束：
 * <ul>
 *   <li>价格必须为正且不超过 10 万元（校园二手合理区间，防止手滑多打零）；</li>
 *   <li>底价必须小于售价，否则"议价"没有意义；</li>
 *   <li>图片最多 5 张，与前端上传组件限制一致。</li>
 * </ul>
 * 注意：{@code status} 不在请求参数里——新发布商品一律 PENDING，由后端固定。
 */
@Data
@Schema(description = "发布商品")
public class ProductCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 64, message = "标题不能超过 64 字")
    private String title;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @DecimalMax(value = "100000.00", message = "价格不能超过 10 万")
    private BigDecimal price;

    @Schema(description = "卖家底价，仅用于议价自动成交判断，不对外展示")
    @DecimalMin(value = "0.00", message = "底价不能为负")
    private BigDecimal minPrice;

    @Schema(description = "是否允许议价：1 允许 / 0 一口价")
    @Min(value = 0, message = "allowBargain 只能为 0 或 1")
    @Max(value = 1, message = "allowBargain 只能为 0 或 1")
    private Integer allowBargain = 1;

    @Size(max = 16, message = "成色描述过长")
    private String conditionLevel = "99新";

    @Size(max = 64, message = "交易地点过长")
    private String location;

    @Size(max = 1000, message = "描述不能超过 1000 字")
    private String description;

    @Schema(description = "图片相对 URL 列表，最多 5 张，第一张作为封面")
    @Size(max = 5, message = "最多上传 5 张图片")
    private List<String> images;
}
