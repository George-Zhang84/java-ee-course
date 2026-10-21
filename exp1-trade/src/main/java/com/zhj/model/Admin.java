package com.zhj.model;

import com.zhj.model.enums.UserRole;

public class Admin extends User {
    private int adminLevel;  // 管理员权限区分

    public Admin() {
    }

    public Admin(String id, String username, String password, String phone) {
        super(id, username, password, phone);
        this.adminLevel = 0;
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public String getMenuTitle() {
        return "管理员菜单";
    }

    public int getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(int adminLevel) {
        this.adminLevel = adminLevel;
    }
}
