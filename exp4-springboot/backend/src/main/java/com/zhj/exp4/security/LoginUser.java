package com.zhj.exp4.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security 的登录主体。
 *
 * <p>authorities 由两部分组成：
 * <ul>
 *   <li>角色：{@code ROLE_USER}、{@code ROLE_SELLER}（由 {@link AuthInfo#getRoles()} 转换）；</li>
 *   <li>权限码：{@code product:publish} 等，直接作为 authority 使用，
 *       因而 {@code @PreAuthorize("hasAuthority('product:publish')")} 可生效。</li>
 * </ul>
 * 之所以同时装配两者，是为了同时支持"按角色"与"按权限点"两种鉴权写法。
 */
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String nickname;
    private final List<String> roles;
    private final List<GrantedAuthority> authorities;

    public LoginUser(AuthInfo authInfo) {
        this.userId = authInfo.getUserId();
        this.username = authInfo.getUsername();
        this.nickname = authInfo.getNickname();
        this.roles = List.copyOf(authInfo.getRoles());
        List<GrantedAuthority> list = new ArrayList<>();
        authInfo.getRoles().forEach(role -> list.add(new SimpleGrantedAuthority("ROLE_" + role)));
        authInfo.getPermissions().forEach(code -> list.add(new SimpleGrantedAuthority(code)));
        this.authorities = List.copyOf(list);
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** 密码校验已由 AuthService 用 BCrypt 单独完成，此处不需要返回密文 */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 被管理员禁用的账号（status=0）在装配 AuthInfo 阶段就已被拒绝，因此这里恒为 true */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
