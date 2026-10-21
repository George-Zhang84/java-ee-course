package com.zhj.model;

import com.zhj.model.enums.UserRole;
import com.zhj.model.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 用户抽象基类
 * 所有角色（买家、卖家、管理员）都继承此类
 */
public abstract class User {

    // ========== 属性 ==========
    private String id;              // 用户唯一编号
    private String username;        // 登录名
    private String password;        // 密码
    private String phone;           // 手机号
    private LocalDateTime registerTime; // 注册时间
    private UserStatus status;      // 账号状态（正常/禁用）

    // ========== 构造方法 ==========
    public User() {
    }

    public User(String id, String username, String password, String phone) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.registerTime = LocalDateTime.now();
        this.status = UserStatus.ACTIVE;
    }

    // ========== 抽象方法（子类必须实现）==========
    /** 返回当前用户的角色 */
    public abstract UserRole getRole();

    /** 返回当前用户看到的主菜单标题 */
    public abstract String getMenuTitle();

    // ========== Getter / Setter ==========
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getRegisterTime() { return registerTime; }
    public void setRegisterTime(LocalDateTime registerTime) { this.registerTime = registerTime; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    // ========== 便捷方法 ==========
    /** 判断账号是否正常可用 */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)",
                getRole(), username, status);
    }
}