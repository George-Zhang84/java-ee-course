package com.zhj.dao;

import com.zhj.infrastructure.DBUtil;
import com.zhj.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findByUsername(String username) {
        String sql = "SELECT * FROM sys_user WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToUser(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public User findById(String id) {
        String sql = "SELECT * FROM sys_user WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToUser(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public List<String> findPermissionsByRole(String roleCode) {
        List<String> permissions = new ArrayList<>();
        String sql = "SELECT p.code FROM sys_permission p " +
                "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
                "WHERE rp.role_code = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) permissions.add(rs.getString("code"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return permissions;
    }

    public boolean insert(User user) {
        String sql = "INSERT INTO sys_user (id, username, password, role_code, balance, shop_name, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRoleCode());
            ps.setDouble(5, user.getBalance() != null ? user.getBalance() : 0.0);
            ps.setString(6, user.getShopName());
            ps.setInt(7, user.getStatus() != null ? user.getStatus() : 1);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public void updateBalance(Connection conn, String userId, double newBalance) throws Exception {
        String sql = "UPDATE sys_user SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    // 多条件查询：关键字 + 角色
    public List<User> findAll(String keyword, String roleCode) {
        List<User> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_user WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND username LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }
        if (roleCode != null && !roleCode.trim().isEmpty()) {
            sql.append("AND role_code = ? ");
            params.add(roleCode);
        }
        sql.append("ORDER BY create_time DESC");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    // 用户列表会直接序列化返回给前端，绝不能把密码散列带出去
                    user.setPassword(null);
                    list.add(user);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public boolean updateStatus(String userId, int status) {
        String sql = "UPDATE sys_user SET status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, status);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // 提取公共映射方法，减少代码重复
    private User mapResultSetToUser(ResultSet rs) throws Exception {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRoleCode(rs.getString("role_code"));
        u.setBalance(rs.getDouble("balance"));
        u.setShopName(rs.getString("shop_name"));
        u.setStatus(rs.getInt("status"));
        return u;
    }
}