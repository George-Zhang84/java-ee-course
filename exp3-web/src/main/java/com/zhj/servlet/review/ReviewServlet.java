package com.zhj.servlet.review;

import com.alibaba.fastjson2.JSONObject;
import com.zhj.dao.OrderDao;
import com.zhj.infrastructure.JsonUtil;
import com.zhj.model.Order;
import com.zhj.model.Review;
import com.zhj.model.User;
import com.zhj.service.ReviewService;
import com.zhj.servlet.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/review")
public class ReviewServlet extends BaseServlet {

    private final ReviewService reviewService = new ReviewService();
    private final OrderDao orderDao = new OrderDao();

    public void submit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        if (!"BUYER".equals(user.getRoleCode())) {
            JsonUtil.error(resp, 403, "只有买家可以评价");
            return;
        }
        JSONObject body = JsonUtil.readRequest(req);
        String orderId = body.getString("orderId");
        Integer rating = body.getInteger("rating");
        String content = body.getString("content");

        String result = reviewService.submitReview(orderId, rating, content, user);
        if ("success".equals(result)) {
            JsonUtil.success(resp, "评价成功");
        } else {
            JsonUtil.error(resp, 400, result);
        }
    }

    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sellerId = req.getParameter("sellerId");
        List<Review> list = reviewService.getReviews(sellerId);
        JsonUtil.success(resp, list);
    }

    public void getByOrder(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        String orderId = req.getParameter("orderId");
        if (orderId == null || orderId.trim().isEmpty()) {
            JsonUtil.error(resp, 400, "订单号不能为空");
            return;
        }
        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.error(resp, 404, "订单不存在");
            return;
        }
        // 只有订单的买家、卖家或管理员可以查看该订单的评价
        boolean allowed = user.getId().equals(order.getBuyerId())
                || user.getId().equals(order.getSellerId())
                || "ADMIN".equals(user.getRoleCode())
                || "SUPER_ADMIN".equals(user.getRoleCode());
        if (!allowed) {
            JsonUtil.error(resp, 403, "无权查看该评价");
            return;
        }
        Review review = reviewService.getReviewByOrderId(orderId);
        JsonUtil.success(resp, review);
    }

    public void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        if (!"ADMIN".equals(user.getRoleCode()) && !"SUPER_ADMIN".equals(user.getRoleCode())) {
            JsonUtil.error(resp, 403, "无权限操作");
            return;
        }
        JSONObject body = JsonUtil.readRequest(req);
        String reviewId = body.getString("reviewId");
        if (reviewId == null || reviewId.trim().isEmpty()) {
            JsonUtil.error(resp, 400, "评价ID不能为空");
            return;
        }
        if (reviewService.deleteReview(reviewId, user)) {
            JsonUtil.success(resp, "评价已删除");
        } else {
            JsonUtil.error(resp, 400, "评价不存在或已被删除");
        }
    }
}
