package com.zhj.service;

import com.zhj.audit.AuditLogger;
import com.zhj.dao.OrderDao;
import com.zhj.dao.ReviewDao;
import com.zhj.infrastructure.IdUtil;
import com.zhj.model.Order;
import com.zhj.model.Review;
import com.zhj.model.User;

import java.util.List;

public class ReviewService {

    private final ReviewDao reviewDao = new ReviewDao();
    private final OrderDao orderDao = new OrderDao();

    /**
     * 提交评价。返回 "success" 或具体失败原因。
     *
     * <p>原实现直接使用前端传来的 sellerId，也不校验订单归属和订单状态，
     * 买家可以给任意订单/任意卖家刷评价。这里全部改成以数据库中的订单为准。</p>
     */
    public String submitReview(String orderId, Integer rating, String content, User buyer) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return "订单号不能为空";
        }
        Order order = orderDao.findById(orderId);
        if (order == null) {
            return "订单不存在";
        }
        if (!buyer.getId().equals(order.getBuyerId())) {
            return "只能评价自己的订单";
        }
        if (!"RECEIVED".equals(order.getStatus())) {
            return "确认收货后才能评价";
        }
        if (rating == null || rating < 1 || rating > 5) {
            return "评分必须在 1-5 星之间";
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            return "评价内容不能为空";
        }
        if (text.length() > 255) {
            return "评价内容不能超过 255 个字符";
        }
        if (reviewDao.existsByOrderId(orderId)) {
            return "该订单已经评价过，请勿重复提交";
        }

        Review review = new Review();
        review.setId(IdUtil.next("R"));
        review.setOrderId(orderId);
        review.setBuyerId(buyer.getId());
        review.setSellerId(order.getSellerId());
        review.setRating(rating);
        review.setContent(text);

        if (reviewDao.insert(review)) {
            AuditLogger.log(buyer.getUsername(), "提交评价", "订单号: " + orderId);
            return "success";
        }
        return "系统异常，评价失败";
    }

    public List<Review> getReviews(String sellerId) {
        return reviewDao.findList(sellerId);
    }

    public Review getReviewByOrderId(String orderId) {
        return reviewDao.findByOrderId(orderId);
    }

    public boolean deleteReview(String reviewId, User admin) {
        boolean success = reviewDao.delete(reviewId);
        if (success) {
            AuditLogger.log(admin.getUsername(), "删除评价", "评价ID: " + reviewId);
        }
        return success;
    }
}
