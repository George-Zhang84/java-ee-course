package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.request.ProductCreateRequest;
import com.zhj.exp4.dto.request.ProductQuery;
import com.zhj.exp4.dto.request.ProductUpdateRequest;
import com.zhj.exp4.dto.response.ProductDetailResponse;
import com.zhj.exp4.dto.response.ProductListResponse;

/**
 * 商品服务。
 *
 * <p>所有涉及"改"的方法都在实现类内部做归属校验（currentUserId 与 sellerId 比对），
 * Controller 上的 {@code @PreAuthorize} 只解决"有没有这个功能权限"，
 * 两者结合才是完整的"功能权限 + 数据权限"。
 */
public interface ProductService {

    /**
     * 公开商品列表：只返回 ON_SALE，支持关键词/分类/价格区间/排序。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ProductListResponse> pagePublic(ProductQuery query);

    /**
     * 我的商品列表：当前用户发布的所有状态商品。
     *
     * @param query 查询条件（status 可选）
     * @return 分页结果
     */
    PageResult<ProductListResponse> pageMine(ProductQuery query);

    /**
     * 商品详情（浏览量 +1；未审核商品仅卖家本人可见）。
     *
     * @param id 商品 id
     * @return 详情
     */
    ProductDetailResponse detail(Long id);

    /**
     * 发布商品：状态固定 PENDING，等待管理员审核。
     *
     * @param request 发布参数
     * @return 新商品 id
     */
    Long create(ProductCreateRequest request);

    /**
     * 编辑商品：仅卖家本人，且商品处于可编辑状态。
     *
     * @param id      商品 id
     * @param request 编辑参数
     */
    void update(Long id, ProductUpdateRequest request);

    /**
     * 上架/下架：仅卖家本人。
     *
     * @param id     商品 id
     * @param status 目标状态（ON_SALE / OFF_SHELF）
     */
    void changeStatus(Long id, String status);

    /**
     * 逻辑删除：仅卖家本人，且商品未被锁定/售出。
     *
     * @param id 商品 id
     */
    void delete(Long id);
}
