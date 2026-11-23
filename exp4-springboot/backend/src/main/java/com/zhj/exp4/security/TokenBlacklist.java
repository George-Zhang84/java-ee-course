package com.zhj.exp4.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌黑名单（内存实现）。
 *
 * <p>JWT 天生无状态，签发后无法"撤回"，但退出登录、修改密码后旧令牌必须失效，
 * 否则安全性无法保证。这里用内存 Map 记录被吊销的令牌及其过期时间：
 * <ul>
 *   <li>键为完整令牌串，值为令牌自身的过期时间戳；</li>
 *   <li>校验时先查黑名单，命中即拒绝；</li>
 *   <li>过期令牌无需保留，每次写入时顺带清理，避免无限增长。</li>
 * </ul>
 *
 * <p>局限：多实例部署时各自持有内存黑名单，吊销不能跨实例生效；
 * 生产环境应换成 Redis + 过期时间。当前课程场景为单体应用，可接受。
 */
@Slf4j
@Component
public class TokenBlacklist {

    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    /**
     * 吊销令牌。
     *
     * @param token     完整 JWT
     * @param expireAt  令牌过期时间戳（毫秒），到点后自动清理
     */
    public void revoke(String token, long expireAt) {
        if (token == null || token.isBlank()) {
            return;
        }
        clearExpired();
        revokedTokens.put(token, expireAt);
    }

    /** 令牌是否已被吊销 */
    public boolean isRevoked(String token) {
        Long expireAt = revokedTokens.get(token);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }

    /** 清理已过期条目，防止内存泄漏 */
    private void clearExpired() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
        log.debug("令牌黑名单清理后剩余 {} 条", revokedTokens.size());
    }

    /** 当前黑名单条数（仅用于测试与监控） */
    public int size() {
        return revokedTokens.size();
    }
}
