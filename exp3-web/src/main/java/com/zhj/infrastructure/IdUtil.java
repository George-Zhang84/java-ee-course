package com.zhj.infrastructure;

import java.util.UUID;

/**
 * 主键生成工具。
 *
 * <p>之前各个 Service 使用 "O" + System.currentTimeMillis() 生成主键，
 * 同一毫秒内的两个并发请求会得到相同 ID，直接触发主键冲突。
 * 这里改用 UUID 截断，保证并发安全，同时长度不超过表字段 varchar(32)。</p>
 */
public final class IdUtil {

    private static final int MAX_LENGTH = 32;

    private IdUtil() {
    }

    public static String next(String prefix) {
        String p = prefix == null ? "" : prefix;
        if (p.length() >= MAX_LENGTH) {
            return p.substring(0, MAX_LENGTH);
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return p + uuid.substring(0, MAX_LENGTH - p.length());
    }
}
