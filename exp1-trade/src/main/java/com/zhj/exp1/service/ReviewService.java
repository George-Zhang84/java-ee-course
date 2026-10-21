package com.zhj.exp1.service;

import com.zhj.exp1.exception.BusinessException;
import com.zhj.exp1.exception.InvalidOperationException;
import com.zhj.exp1.model.Order;
import com.zhj.exp1.model.Review;
import com.zhj.exp1.model.Seller;
import com.zhj.exp1.model.enums.OrderStatus;
import com.zhj.exp1.repository.DataStore;
import com.zhj.exp1.util.IdGenerator;

import java.util.List;

/**
 * 评价业务逻辑层
 */
public class ReviewService {

    private final DataStore store = DataStore.getInstance();

    /**
     * 买家对已收货订单添加评价
     */
    public Review addReview(String buyerId, String orderId, int rating, String content) {
        Order order = store.findOrderById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new InvalidOperationException("无权评价他人的订单");
        }
        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new InvalidOperationException("只有【已收货】订单可以评价");
        }
        if (rating < 1 || rating > 5) {
            throw new InvalidOperationException("评分必须在 1-5 之间");
        }
        // 检查是否已评价过
        if (isOrderReviewed(orderId)) {
            throw new InvalidOperationException("该订单已评价过，不能重复评价");
        }

        Review review = new Review(
                IdGenerator.nextReviewId(),
                orderId,
                buyerId,
                order.getSellerId(),
                rating,
                content
        );
        store.addReview(review);

        // 更新卖家的平均评分
        updateSellerRating(order.getSellerId());

        return review;
    }

    /**
     * 判断订单是否已被评价
     */
    public boolean isOrderReviewed(String orderId) {
        return store.getAllReviews().stream()
                .anyMatch(r -> r.getOrderId().equals(orderId));
    }

    /**
     * 重新计算卖家平均评分
     */
    private void updateSellerRating(String sellerId) {
        List<Review> reviews = store.findReviewsBySeller(sellerId);
        if (reviews.isEmpty()) return;

        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        Seller seller = (Seller) store.findUserById(sellerId);
        if (seller != null) {
            seller.setRating(Math.round(avg * 10) / 10.0);  // 保留一位小数
        }
    }

    // ================== 查询 ==================

    public List<Review> listBySeller(String sellerId) {
        return store.findReviewsBySeller(sellerId);
    }

    public List<Review> listByBuyer(String buyerId) {
        return store.findReviewsByBuyer(buyerId);
    }

    public List<Review> listAll() {
        return store.getAllReviews();
    }

    /**
     * 管理员删除违规评价
     */
    public void deleteReview(String reviewId) {
        Review review = store.findReviewById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        String sellerId = review.getSellerId();
        store.removeReview(reviewId);
        updateSellerRating(sellerId);
    }
}