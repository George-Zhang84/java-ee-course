package com.zhj.exp4.service;

import com.zhj.exp4.dto.response.CategoryResponse;

import java.util.List;

/**
 * 商品分类服务。
 */
public interface CategoryService {

    /**
     * 查询所有启用中的分类，按 sort 升序。
     *
     * @return 分类列表
     */
    List<CategoryResponse> listEnabled();

    /**
     * 校验分类存在且启用；不存在时抛出业务异常。
     *
     * @param categoryId 分类 id
     * @return 分类名称
     */
    String requireEnabled(Long categoryId);
}
