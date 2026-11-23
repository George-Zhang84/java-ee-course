package com.zhj.exp4.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 工具。
 *
 * <p>直接取 {@code getRemoteAddr()} 在反向代理场景下拿到的是代理 IP，
 * 因此优先读取代理链头部，并做长度与格式的基本清洗（防止伪造超长头部写入审计表）。
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final int MAX_LENGTH = 64;

    private IpUtil() {
    }

    /**
     * 获取真实客户端 IP。
     *
     * @param request 当前请求
     * @return IP 字符串，无法获取时返回 unknown
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能是 "client, proxy1, proxy2"，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null) {
            return UNKNOWN;
        }
        // 本机回环统一显示为 127.0.0.1，便于演示时辨认
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.length() > MAX_LENGTH ? ip.substring(0, MAX_LENGTH) : ip;
    }

    private static boolean isInvalid(String ip) {
        return ip == null || ip.isBlank() || UNKNOWN.equalsIgnoreCase(ip);
    }
}
