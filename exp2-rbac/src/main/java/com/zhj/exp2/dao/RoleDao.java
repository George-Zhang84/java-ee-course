package com.zhj.exp2.dao;

import com.zhj.exp2.infrastructure.exception.BusinessException;
import com.zhj.exp2.model.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleDao extends BaseDao {

    public List<Role> findAll() {
        List<Role> list = new ArrayList<>();
        String sql = "SELECT id, role_code, role_name FROM sys_role";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Role r = new Role();
                r.setId(rs.getInt("id"));
                r.setRoleCode(rs.getString("role_code"));
                r.setRoleName(rs.getString("role_name"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 为角色分配权限
    public void assignPermissions(int roleId, List<Integer> permIds) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // 开启事务
            try {
                // 1. 清除原有权限
                try (PreparedStatement pst = conn.prepareStatement("DELETE FROM sys_role_perm WHERE role_id = ?")) {
                    pst.setInt(1, roleId);
                    pst.executeUpdate();
                }
                // 2. 插入新权限
                if (permIds != null && !permIds.isEmpty()) {
                    try (PreparedStatement pst = conn.prepareStatement("INSERT INTO sys_role_perm (role_id, perm_id) VALUES (?, ?)")) {
                        for (int pid : permIds) {
                            pst.setInt(1, roleId);
                            pst.setInt(2, pid);
                            pst.addBatch();
                        }
                        pst.executeBatch();
                    }
                }
                conn.commit(); // 提交事务
            } catch (Exception e) {
                conn.rollback(); // 出错回滚
                throw e;
            }
        } catch (Exception e) {
            // 将底层 SQL 异常转化为业务异常，并友好提示
            throw new BusinessException("数据库操作失败：分配权限失败，请检查角色或权限ID是否存在 (" + e.getMessage() + ")");
        }
    }

    public String getRoleCodeById(int roleId) {
        String sql = "SELECT role_code FROM sys_role WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("role_code");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}