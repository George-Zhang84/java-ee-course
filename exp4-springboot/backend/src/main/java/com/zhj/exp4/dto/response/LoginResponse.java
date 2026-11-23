package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 登录/注册成功的响应。
 *
 * <p>返回权限码列表而非只返回角色：前端用权限码渲染菜单与按钮，
 * 后端用同一份权限码做接口校验，前后端判断口径完全一致。
 */
@Data
@Builder
public class LoginResponse {

    /** JWT 令牌，前端存 localStorage 并放入 Authorization 头 */
    private String token;

    /** 令牌有效期（秒），前端可用于提前提醒续期 */
    private long expiresIn;

    private UserInfoResponse user;

    /** 权限码列表，驱动前端菜单与按钮 */
    private Set<String> permissions;

    /** 角色编码列表，如 [USER, SELLER] */
    private List<String> roles;
}
