package com.zhj.exp4.security;

import com.zhj.exp4.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 工具单元测试。
 *
 * <p>覆盖签发、解析、过期与密钥不匹配四类场景，
 * 其中"密钥不匹配必须验签失败"是防止伪造令牌的底线。
 */
@DisplayName("JWT 签发与校验")
class JwtUtilTest {

    private static final String SECRET = "exp4-unit-test-secret-key-0123456789";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(SECRET);
        props.getJwt().setExpireSeconds(3600L);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    @DisplayName("签发后能正确解析出用户 id 与用户名")
    void generateAndParse() {
        String token = jwtUtil.generate(42L, "buyer1");
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "JWT 应为 header.payload.signature 三段");

        Claims claims = jwtUtil.parse(token);
        assertEquals(42L, jwtUtil.userIdOf(claims));
        assertEquals("buyer1", jwtUtil.usernameOf(claims));
        assertTrue(jwtUtil.expirationOf(claims) > System.currentTimeMillis());
    }

    @Test
    @DisplayName("密钥不同则验签失败（防伪造）")
    void rejectForgedToken() {
        String token = jwtUtil.generate(1L, "admin");

        AppProperties other = new AppProperties();
        other.getJwt().setSecret("another-secret-key-0123456789abcdef");
        JwtUtil anotherUtil = new JwtUtil(other);

        assertThrows(JwtException.class, () -> anotherUtil.parse(token));
    }

    @Test
    @DisplayName("过期令牌解析失败")
    void rejectExpiredToken() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(SECRET);
        // 负数有效期 => 签发即过期
        props.getJwt().setExpireSeconds(-10L);
        JwtUtil expiredUtil = new JwtUtil(props);
        String token = expiredUtil.generate(1L, "admin");
        assertThrows(JwtException.class, () -> jwtUtil.parse(token));
    }

    @Test
    @DisplayName("篡改载荷或签名结构非法时解析失败")
    void rejectMalformedToken() {
        assertThrows(Exception.class, () -> jwtUtil.parse("not-a-jwt"));
        String token = jwtUtil.generate(1L, "admin");
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(JwtException.class, () -> jwtUtil.parse(tampered));
    }
}
