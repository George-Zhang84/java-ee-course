package com.zhj.exp2.dao;

import com.zhj.exp2.model.Permission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PermissionDao extends BaseDao {
    public List<Permission> findAll() {
        List<Permission> list = new ArrayList<>();
        String sql = "SELECT id, perm_code, perm_name, perm_type FROM sys_permission ORDER BY id";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Permission(
                        rs.getInt("id"),
                        rs.getString("perm_code"),
                        rs.getString("perm_name"),
                        rs.getString("perm_type")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}