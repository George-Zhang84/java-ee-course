package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.request.ProductQuery;
import com.zhj.exp4.dto.response.ProductListResponse;

/**
 * 收藏夹服务。
 */
public interface FavoriteService {

    /**
     * 收藏商品（幂等：已收藏时直接返回成功）。
     *
     * @param productId 商品 id
     */
    void add(Long productId);

    /**
     * 取消收藏（幂等：未收藏也不报错）。
     *
     * @param productId 商品 id
     */
    void remove(Long productId);

    /**
     * 我的收藏分页。
     *
     * @param query 分页参数
     * @return 收藏的商品列表
     */
    PageResult<ProductListResponse> pageMine(ProductQuery query);
}
