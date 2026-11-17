package com.zhj.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接工具。
 *
 * <p>数据库地址/账号/密码支持通过 JVM 参数或环境变量覆盖，
 * 避免把生产环境的真实密码写死在代码里：
 * <pre>
 *   -Dexp3.db.url=... -Dexp3.db.user=... -Dexp3.db.password=...
 *   或环境变量 EXP3_DB_URL / EXP3_DB_USER / EXP3_DB_PASSWORD
 * </pre>
 * 没有配置时回落到本地开发默认值。</p>
 */
public class DBUtil {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/exp3_trade_rbac"
                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false&allowPublicKeyRetrieval=true"
                    + "&connectTimeout=5000&socketTimeout=15000";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123456";

    private static final String URL = resolve("exp3.db.url", "EXP3_DB_URL", DEFAULT_URL);
    private static final String USER = resolve("exp3.db.user", "EXP3_DB_USER", DEFAULT_USER);
    private static final String PASSWORD = resolve("exp3.db.password", "EXP3_DB_PASSWORD", DEFAULT_PASSWORD);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL驱动加载失败", e);
        }
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String resolve(String propertyKey, String envKey, String defaultValue) {
        String value = System.getProperty(propertyKey);
        if (isBlank(value)) {
            value = System.getenv(envKey);
        }
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // 简单的连通性自测入口，方便课程演示时快速排查环境问题
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("数据库连接成功！");
        } catch (SQLException e) {
            System.out.println("数据库连接失败：" + e.getMessage());
        }
    }
}
