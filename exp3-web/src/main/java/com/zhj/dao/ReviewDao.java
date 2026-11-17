package com.zhj.dao;

import com.zhj.infrastructure.DBUtil;
import com.zhj.model.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReviewDao {

    private static final Logger logger = LoggerFactory.getLogger(ReviewDao.class);
    public boolean insert(Review review) {
        String sql = "INSERT INTO review (id, order_id, buyer_id, seller_id, rating, content) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, review.getId());
            ps.setString(2, review.getOrderId());
            ps.setString(3, review.getBuyerId());
            ps.setString(4, review.getSellerId());
            ps.setInt(5, review.getRating());
            ps.setString(6, review.getContent());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean existsByOrderId(String orderId) {
        String sql = "SELECT COUNT(*) FROM review WHERE order_id = ? AND status = 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // 按卖家查询（过滤已删除）
    public List<Review> findList(String sellerId) {
        List<Review> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.*, p.name AS product_name, u.username AS buyer_name " +
                        "FROM review r " +
                        "JOIN `order` o ON r.order_id = o.id " +
                        "JOIN product p ON o.product_id = p.id " +
                        "JOIN sys_user u ON r.buyer_id = u.id " +
                        "WHERE r.status = 1 "
        );
        if (sellerId != null) sql.append("AND r.seller_id = ? ");
        sql.append("ORDER BY r.create_time DESC");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (sellerId != null) ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getString("id"));
                    r.setOrderId(rs.getString("order_id"));
                    r.setBuyerId(rs.getString("buyer_id"));
                    r.setSellerId(rs.getString("seller_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setContent(rs.getString("content"));
                    r.setCreateTime(rs.getString("create_time"));
                    r.setProductName(rs.getString("product_name"));
                    r.setBuyerName(rs.getString("buyer_name"));
                    list.add(r);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // 根据订单号查询单条评价（买家查看用）
    public Review findByOrderId(String orderId) {
        String sql = "SELECT * FROM review WHERE order_id = ? AND status = 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getString("id"));
                    r.setRating(rs.getInt("rating"));
                    r.setContent(rs.getString("content"));
                    r.setCreateTime(rs.getString("create_time"));
                    return r;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // 软删除：只允许把“有效评价”改成 0，重复删除不会误改已删除记录
    public boolean delete(String reviewId) {
        String sql = "UPDATE review SET status = 0 WHERE id = ? AND status = 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reviewId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            logger.error("删除评价失败, reviewId={}", reviewId, e);
            return false;
        }
    }
}