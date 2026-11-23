package com.zhj.exp4.security;

import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>执行流程（位于 Spring Security 过滤器链中，早于授权过滤器）：
 * <ol>
 *   <li>从 {@code Authorization: Bearer xxx} 取出令牌，没有则直接放行（由后续规则决定 401 还是公开访问）；</li>
 *   <li>校验签名与有效期；</li>
 *   <li>查黑名单，已吊销的令牌视为未登录；</li>
 *   <li>查用户并确认 status=1（管理员可即时封禁）；</li>
 *   <li>从权限缓存装配 {@link LoginUser} 并写入 SecurityContext。</li>
 * </ol>
 *
 * <p>任何一步失败都"静默跳过"而不是直接抛错：让 AuthenticationEntryPoint 统一返回 401，
 * 避免暴露"令牌过期"与"签名错误"的区别给攻击者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final SysUserMapper userMapper;
    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            authenticate(request, token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        if (tokenBlacklist.isRevoked(token)) {
            log.debug("令牌已吊销，忽略");
            return;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            Long userId = jwtUtil.userIdOf(claims);
            if (userId == null) {
                return;
            }
            SysUser user = userMapper.selectById(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                log.debug("用户不存在或已被禁用：userId={}", userId);
                return;
            }
            AuthInfo authInfo = permissionService.load(user);
            LoginUser loginUser = new LoginUser(authInfo);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException e) {
            // 令牌非法/过期：不设置认证信息，交给 EntryPoint 返回 401
            log.debug("JWT 校验失败：{}", e.getMessage());
        }
    }
}
