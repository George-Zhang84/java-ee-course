package com.zhj.dao;

import com.zhj.infrastructure.DBUtil;
import com.zhj.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private static final Logger logger = LoggerFactory.getLogger(OrderDao.class);

    /**
     * 创建订单（事务控制）。
     *
     * <p>并发安全要点：
     * <ol>
     *   <li>商品状态更新带 {@code status='ON_SALE'} 条件，两个买家同时抢最后一件时，
     *       只有一个 UPDATE 能影响 1 行，另一个直接回滚，避免超卖；</li>
     *   <li>余额扣减用 {@code balance = balance - ?} 且带 {@code balance >= ?} 条件，
     *       不依赖 Session 中的旧余额，避免同一用户并发下单把余额扣成负数；</li>
     *   <li>三步操作在同一事务中，任何一步失败全部回滚。</li>
     * </ol>
     *
     * @return "success" 或具体失败原因
     */
    public String createOrder(Order order) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 1. 抢占商品：只有仍在售的商品才能被买走
            String sql1 = "UPDATE product SET status = 'SOLD' WHERE id = ? AND status = 'ON_SALE'";
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1, order.getProductId());
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return "手慢了，商品已被其他买家抢购或已下架";
                }
            }

            // 2. 扣减买家余额（在数据库里做减法，并校验余额充足）
            String sql2 = "UPDATE sys_user SET balance = balance - ? WHERE id = ? AND balance >= ?";
            try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                ps.setDouble(1, order.getAmount());
                ps.setString(2, order.getBuyerId());
                ps.setDouble(3, order.getAmount());
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return "余额不足，请先充值";
                }
            }

            // 3. 插入订单记录
            String sql3 = "INSERT INTO `order` (id, buyer_id, seller_id, product_id, amount, status) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql3)) {
                ps.setString(1, order.getId());
                ps.setString(2, order.getBuyerId());
                ps.setString(3, order.getSellerId());
                ps.setString(4, order.getProductId());
                ps.setDouble(5, order.getAmount());
                ps.setString(6, order.getStatus());
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("订单写入失败");
                }
            }

            conn.commit();
            return "success";
        } catch (Exception e) {
            logger.error("创建订单失败, orderId={}", order.getId(), e);
            rollbackQuietly(conn);
            return "系统异常，下单失败";
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * 卖家确认发货：PENDING -> CONFIRMED。
     * 带上 seller_id 与 status 条件，天然防止越权与重复操作。
     */
    public boolean confirmOrder(String orderId, String sellerId) {
        String sql = "UPDATE `order` SET status = 'CONFIRMED' WHERE id = ? AND seller_id = ? AND status = 'PENDING'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, sellerId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            logger.error("确认订单失败, orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 买家确认收货：CONFIRMED -> RECEIVED，并在同一事务中把货款打给卖家。
     *
     * <p>原实现只改了订单状态，卖家的钱永远不会到账。这里补上资金结算，
     * 并用 {@code FOR UPDATE} + 状态条件保证幂等：重复提交不会重复打款。</p>
     */
    public boolean completeOrder(String orderId, String buyerId) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String sellerId;
            double amount;
            String lockSql = "SELECT seller_id, amount FROM `order` "
                    + "WHERE id = ? AND buyer_id = ? AND status = 'CONFIRMED' FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setString(1, orderId);
                ps.setString(2, buyerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    sellerId = rs.getString("seller_id");
                    amount = rs.getDouble("amount");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE `order` SET status = 'RECEIVED' WHERE id = ?")) {
                ps.setString(1, orderId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("订单状态更新失败");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE sys_user SET balance = balance + ? WHERE id = ?")) {
                ps.setDouble(1, amount);
                ps.setString(2, sellerId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("卖家余额入账失败");
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            logger.error("确认收货失败, orderId={}", orderId, e);
            rollbackQuietly(conn);
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    public Order findById(String orderId) {
        String sql = "SELECT o.*, p.name AS product_name, u.username AS buyer_name, s.username AS seller_name, "
                + "EXISTS(SELECT 1 FROM review r WHERE r.order_id = o.id AND r.status = 1) AS is_reviewed "
                + "FROM `order` o "
                + "JOIN product p ON o.product_id = p.id "
                + "JOIN sys_user u ON o.buyer_id = u.id "
                + "JOIN sys_user s ON o.seller_id = s.id "
                + "WHERE o.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        } catch (Exception e) {
            logger.error("查询订单失败, orderId={}", orderId, e);
        }
        return null;
    }

    /**
     * 查询订单列表（买家看到自己买的，卖家看到自己卖的）。
     * 是否已评价用 EXISTS 子查询，避免 LEFT JOIN 产生重复行。
     */
    public List<Order> findList(String userId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, p.name AS product_name, u.username AS buyer_name, s.username AS seller_name, "
                + "EXISTS(SELECT 1 FROM review r WHERE r.order_id = o.id AND r.status = 1) AS is_reviewed "
                + "FROM `order` o "
                + "JOIN product p ON o.product_id = p.id "
                + "JOIN sys_user u ON o.buyer_id = u.id "
                + "JOIN sys_user s ON o.seller_id = s.id "
                + "WHERE o.buyer_id = ? OR o.seller_id = ? "
                + "ORDER BY o.create_time DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (Exception e) {
            logger.error("查询订单列表失败, userId={}", userId, e);
        }
        return list;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getString("id"));
        o.setBuyerId(rs.getString("buyer_id"));
        o.setSellerId(rs.getString("seller_id"));
        o.setProductId(rs.getString("product_id"));
        o.setAmount(rs.getDouble("amount"));
        o.setStatus(rs.getString("status"));
        o.setCreateTime(rs.getString("create_time"));
        o.setProductName(rs.getString("product_name"));
        o.setBuyerName(rs.getString("buyer_name"));
        o.setSellerName(rs.getString("seller_name"));
        o.setReviewed(rs.getInt("is_reviewed") == 1);
        return o;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                logger.warn("事务回滚失败", e);
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("连接关闭失败", e);
            }
        }
    }
}
