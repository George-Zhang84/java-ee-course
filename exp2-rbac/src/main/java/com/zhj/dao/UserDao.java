package com.zhj.dao;

import com.zhj.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class UserDao extends BaseDao {

    /**
     * 根据用户名查询用户 (用于登录)
     */
    public User findByUsername(String username) {
        String sql = "SELECT id, username, password, status FROM sys_user WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setStatus(rs.getInt("status"));
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 查询用户拥有的所有权限码
     */
    public Set<String> findPermissionsByUserId(int userId) {
        Set<String> perms = new HashSet<>();
        String sql = "SELECT p.perm_code FROM sys_permission p " +
                "JOIN sys_role_perm rp ON p.id = rp.perm_id " +
                "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
                "WHERE ur.user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    perms.add(rs.getString("perm_code"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return perms;
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM sys_user WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 查找所有用户，并关联查询出角色名称用于展示
     */
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.status, r.role_name " +
                "FROM sys_user u " +
                "LEFT JOIN sys_user_role ur ON u.id = ur.user_id " +
                "LEFT JOIN sys_role r ON ur.role_id = r.id " +
                "ORDER BY u.id";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setStatus(rs.getInt("status"));
                u.setRoleName(rs.getString("role_name"));
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 删除用户
     */
    public boolean deleteById(int id) {
        String sql = "DELETE FROM sys_user WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 插入新用户，并同时分配角色（事务控制）
     */
    public boolean insert(User user, int roleId) {
        String insertUserSql = "INSERT INTO sys_user (username, password, status) VALUES (?, ?, ?)";
        String insertUserRoleSql = "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newUserId = 0;
                try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, user.getUsername());
                    pstmt.setString(2, user.getPassword());
                    pstmt.setInt(3, user.getStatus());
                    pstmt.executeUpdate();
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) newUserId = rs.getInt(1);
                    }
                }

                if (newUserId > 0) {
                    try (PreparedStatement pstmt = conn.prepareStatement(insertUserRoleSql)) {
                        pstmt.setInt(1, newUserId);
                        pstmt.setInt(2, roleId);
                        pstmt.executeUpdate();
                    }
                } else {
                    throw new SQLException("获取新用户ID失败");
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 更新用户状态 (启用/禁用)
     */
    public boolean updateStatus(int userId, int newStatus) {
        String sql = "UPDATE sys_user SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newStatus);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据用户ID查询其角色编码
     */
    public String getRoleCodeByUserId(int userId) {
        String sql = "SELECT r.role_code FROM sys_role r " +
                "JOIN sys_user_role ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("role_code");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}