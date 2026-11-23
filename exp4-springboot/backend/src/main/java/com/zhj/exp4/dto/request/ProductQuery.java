package com.zhj.exp4.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品搜索/分页查询参数（GET 请求，由 Spring 自动从 query string 绑定）。
 *
 * <p>分页参数做了边界保护：{@code page} 最小为 1，{@code size} 限制在 1~50，
 * 避免前端传入超大 size 造成慢查询（MyBatis-Plus 侧还有 maxLimit 兜底）。
 */
@Data
@Schema(description = "商品搜索参数")
public class ProductQuery {

    @Schema(description = "关键词，匹配标题或描述")
    private String keyword;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "最低价")
    private BigDecimal minPrice;

    @Schema(description = "最高价")
    private BigDecimal maxPrice;

    /**
     * 排序方式，只允许白名单值，防止把前端参数直接拼进 order by 造成注入。
     * 可选：newest（默认）/ price_asc / price_desc / views
     */
    @Schema(description = "排序：newest / price_asc / price_desc / views")
    private String sort;

    @Schema(description = "页码，从 1 开始")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50")
    private Integer size = 10;

    @Schema(description = "状态筛选（仅我的商品接口使用）")
    private String status;

    /** 归一化页码，防止 page<=0 导致 SQL 偏移量为负 */
    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    /** 归一化 size，防止过大分页 */
    public int safeSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }
}
