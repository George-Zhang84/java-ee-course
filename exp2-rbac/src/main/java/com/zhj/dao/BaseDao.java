package com.zhj.dao;

import com.zhj.infrastructure.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class BaseDao {
    /**
     * 子类通用的获取连接方法
     */
    protected Connection getConnection() throws SQLException {
        return DBUtil.getConnection();
    }
}