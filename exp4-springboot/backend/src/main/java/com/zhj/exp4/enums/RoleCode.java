package com.zhj.exp4.enums;

import lombok.Getter;

/**
 * 内置角色编码。
 *
 * <p>Spring Security 约定：数据库中的角色编码在装配权限时加 {@code ROLE_} 前缀，
 * 以便同时支持 {@code hasRole('ADMIN')} 与 {@code hasAuthority('product:audit')} 两种写法。
 */
@Getter
public enum RoleCode {

    SUPER_ADMIN("超级管理员"),
    ADMIN("管理员"),
    USER("普通用户"),
    SELLER("卖家");

    private final String description;

    RoleCode(String description) {
        this.description = description;
    }

    /** 转换为 Spring Security 的 authority 字符串 */
    public String authority() {
        return "ROLE_" + name();
    }
}
