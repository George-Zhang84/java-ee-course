package com.zhj.exp4.dto.response;

import lombok.Data;

/**
 * 分类占比数据（饼图）。
 */
@Data
public class CategoryStat {

    /** 分类名称 */
    private String name;

    /** 该分类下商品数量 */
    private Long productCount;
}
