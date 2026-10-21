package com.zhj.model;

import com.zhj.model.enums.UserRole;

public class Buyer extends User {

    private double balance;  // 账户余额

    public Buyer() {
    }

    public Buyer(String id, String username, String password, String phone) {
        super(id, username, password, phone);  // 调用父类构造方法
        this.balance = 0.0;
    }

    @Override
    public UserRole getRole() {
        return UserRole.BUYER;
    }

    @Override
    public String getMenuTitle() {
        return "买家菜单";
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
