package com.zhj.dao;

import com.zhj.infrastructure.DBUtil;
import com.zhj.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    private static final Logger logger = LoggerFactory.getLogger(ProductDao.class);

    // 分页 + 条件查询 + 排序
    public List<Product> findList(String keyword, String category, String sortBy, int page, int pageSize) {
        List<Product> list = new ArrayList<>();
        // 联表查询，顺便把卖家的名字查出来
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, u.username AS seller_name FROM product p " +
                        "JOIN sys_user u ON p.seller_id = u.id " +
                        "WHERE p.status = 'ON_SALE' "
        );
        List<Object> params = new ArrayList<>();

        // 1. 关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND p.name LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }
        // 2. 分类筛选
        if (category != null && !category.trim().isEmpty() && !"全部".equals(category)) {
            sql.append("AND p.category = ? ");
            params.add(category);
        }

        // 3. 动态排序
        if ("price_asc".equals(sortBy)) {
            sql.append("ORDER BY p.price ASC ");
        } else if ("price_desc".equals(sortBy)) {
            sql.append("ORDER BY p.price DESC ");
        } else {
            sql.append("ORDER BY p.create_time DESC "); // 默认最新
        }

        // 4. 分页限制
        sql.append("LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // 动态注入参数
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getString("id"));
                    p.setSellerId(rs.getString("seller_id"));
                    p.setName(rs.getString("name"));
                    p.setCategory(rs.getString("category"));
                    p.setPrice(rs.getDouble("price"));
                    p.setDescription(rs.getString("description"));
                    p.setStatus(rs.getString("status"));
                    p.setViews(rs.getInt("views"));
                    p.setCreateTime(rs.getString("create_time"));
                    p.setSellerName(rs.getString("seller_name"));
                    list.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 统计符合条件的总记录数（用于计算总页数）
    public int count(String keyword, String category) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM product WHERE status = 'ON_SALE' ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND name LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }
        if (category != null && !category.trim().isEmpty() && !"全部".equals(category)) {
            sql.append("AND category = ? ");
            params.add(category);
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Product findById(String id) {
        String sql = "SELECT p.*, u.username AS seller_name FROM product p " +
                "JOIN sys_user u ON p.seller_id = u.id WHERE p.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getString("id"));
                    p.setSellerId(rs.getString("seller_id"));
                    p.setName(rs.getString("name"));
                    p.setCategory(rs.getString("category"));
                    p.setPrice(rs.getDouble("price"));
                    p.setDescription(rs.getString("description"));
                    p.setStatus(rs.getString("status"));
                    p.setViews(rs.getInt("views"));
                    p.setCreateTime(rs.getString("create_time"));
                    p.setSellerName(rs.getString("seller_name"));
                    return p;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // 卖家发布商品
    public boolean insert(Product product) {
        String sql = "INSERT INTO product (id, seller_id, name, category, price, description, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getId());
            ps.setString(2, product.getSellerId());
            ps.setString(3, product.getName());
            ps.setString(4, product.getCategory());
            ps.setDouble(5, product.getPrice());
            ps.setString(6, product.getDescription());
            ps.setString(7, product.getStatus());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // 查询卖家自己的商品（含所有状态）
    public List<Product> findBySellerId(String sellerId) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product WHERE seller_id = ? ORDER BY create_time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getString("id"));
                    p.setSellerId(rs.getString("seller_id"));
                    p.setName(rs.getString("name"));
                    p.setCategory(rs.getString("category"));
                    p.setPrice(rs.getDouble("price"));
                    p.setDescription(rs.getString("description"));
                    p.setStatus(rs.getString("status"));
                    p.setCreateTime(rs.getString("create_time"));
                    list.add(p);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // 管理员查询所有待审核商品
    public List<Product> findPending() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, u.username AS seller_name FROM product p " +
                "JOIN sys_user u ON p.seller_id = u.id " +
                "WHERE p.status = 'PENDING' ORDER BY p.create_time ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getString("id"));
                p.setSellerId(rs.getString("seller_id"));
                p.setName(rs.getString("name"));
                p.setCategory(rs.getString("category"));
                p.setPrice(rs.getDouble("price"));
                p.setDescription(rs.getString("description"));
                p.setStatus(rs.getString("status"));
                p.setCreateTime(rs.getString("create_time"));
                p.setSellerName(rs.getString("seller_name"));
                list.add(p);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // 审核商品：只有待审核(PENDING)的商品才能被审核，避免重复审核或越权改价态
    public boolean audit(String productId, String status) {
        String sql = "UPDATE product SET status = ? WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, productId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            logger.error("审核商品失败, productId={}", productId, e);
            return false;
        }
    }
}