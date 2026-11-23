package com.zhj.exp4.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 配置（前后端分离、无状态 JWT）。
 *
 * <p>三件事：
 * <ol>
 *   <li><b>认证</b>：不启用表单登录与 Basic，改由 {@link JwtAuthenticationFilter} 解析令牌；</li>
 *   <li><b>授权</b>：开放的接口显式 permitAll，其余一律需认证；方法级再用
 *       {@code @PreAuthorize} 做精细控制（{@code @EnableMethodSecurity} 开启）；</li>
 *   <li><b>异常</b>：401/403 都返回统一 JSON，前端只认 code。</li>
 * </ol>
 *
 * <p>关闭 CSRF 的理由：令牌放在请求头而非 Cookie，浏览器不会自动携带，
 * 不存在 CSRF 攻击面；若改回 Cookie 存储则必须重新开启。
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    /** 密码编码器：BCrypt 自带随机盐，同一明文每次加密结果不同（实验二 MD5 无盐的升级点） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 预检请求直接放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 认证入口
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        // 游客可浏览商品与分类
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        // 静态资源与文档
                        .requestMatchers("/uploads/**", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // 其余全部需要登录
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
