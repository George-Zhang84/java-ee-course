package com.zhj.exp4.dto.response;

import com.zhj.exp4.entity.Category;
import lombok.Builder;
import lombok.Data;

/**
 * 分类响应。只下发启用中的分类，停用分类不会出现在发布表单里。
 */
@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String icon;
    private Integer sort;

    public static CategoryResponse of(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .sort(category.getSort())
                .build();
    }
}
