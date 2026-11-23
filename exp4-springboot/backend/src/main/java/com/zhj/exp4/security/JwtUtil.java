package com.zhj.exp4.security;

import com.zhj.exp4.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与校验。
 *
 * <p>为什么用 JWT 而不是 Session：
 * <ul>
 *   <li>前后端分离后，浏览器只存 token，服务端无会话状态，天然支持多实例扩容；</li>
 *   <li>移动端/第三方也能直接复用同一套认证；</li>
 *   <li>令牌自包含用户标识，减少每次请求查库（权限仍走缓存，兼顾实时性）。</li>
 * </ul>
 *
 * <p>令牌只放"身份"（uid、username），不放权限码：权限变更后用户无需重新登录，
 * 且令牌体积可控。签名算法 HS256，密钥来自配置（生产用 JWT_SECRET 覆盖）。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_USERNAME = "username";

    private final AppProperties appProperties;

    /** 由配置密钥派生的 HMAC-SHA 密钥，长度不足 32 字节时 jjwt 会直接抛出异常（启动即暴露问题） */
    private SecretKey signingKey() {
        String secret = appProperties.getJwt().getSecret();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发令牌。
     *
     * @param userId   用户 id
     * @param username 登录名
     * @return 紧凑格式的 JWT 字符串
     */
    public String generate(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + appProperties.getJwt().getExpireSeconds() * 1000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_UID, userId)
                .claim(CLAIM_USERNAME, username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    /**
     * 校验并解析令牌。
     *
     * @param token 原始令牌（不含 Bearer 前缀）
     * @return 载荷
     * @throws JwtException 签名不合法、已过期或格式错误时抛出，由调用方决定如何处理
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从载荷中取用户 id */
    public Long userIdOf(Claims claims) {
        return claims.get(CLAIM_UID, Long.class);
    }

    /** 从载荷中取登录名 */
    public String usernameOf(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /** 令牌过期时间戳（毫秒），用于黑名单清理 */
    public long expirationOf(Claims claims) {
        return claims.getExpiration().getTime();
    }
}
