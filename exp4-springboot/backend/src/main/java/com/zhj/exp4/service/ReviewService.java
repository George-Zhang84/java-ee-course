package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.request.ReviewCreateRequest;
import com.zhj.exp4.dto.response.ReviewResponse;

/**
 * 评价服务。
 *
 * <p>规则：订单完成后买卖双方各可评价一次（唯一索引保证），
 * 差评会影响被评价人的信用分。
 */
public interface ReviewService {

    /**
     * 发表评价。
     *
     * @param request 评价参数
     */
    void create(ReviewCreateRequest request);

    /** 商品详情页展示的评价（只取买家评卖家） */
    PageResult<ReviewResponse> pageByProduct(Long productId, int page, int size);

    /** 我发出的评价 */
    PageResult<ReviewResponse> pageMine(int page, int size);

    /**
     * 我收到的评价。
     *
     * <p>买家评卖家会展示在商品详情页，而卖家评买家原先没有展示入口，
     * 导致"卖家评价了买家但买家看不到"。这里统一提供"我收到的评价"。
     */
    PageResult<ReviewResponse> pageReceived(int page, int size);
}
