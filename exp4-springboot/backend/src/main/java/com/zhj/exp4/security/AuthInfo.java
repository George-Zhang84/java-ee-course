package com.zhj.exp4.security;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 当前登录用户的权限快照。
 *
 * <p>由 {@link PermissionService} 从数据库装配后放入 Caffeine 缓存，
 * 缓存键是用户 id，因此改动角色权限后必须调用 {@code evict(userId)} 清理。
 */
@Data
@Builder
public class AuthInfo {

    private Long userId;

    private String username;

    private String nickname;

    /** 角色编码，如 [USER, SELLER] */
    private List<String> roles;

    /** 权限码集合，如 [product:publish, order:pay] */
    private Set<String> permissions;
}
