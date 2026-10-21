package com.zhj.model;

import com.zhj.model.enums.UserRole;

public class Seller extends User {
    private String shopName;
    private double rating;
    private int soldCount;  // 卖家累计销量

    public Seller() {
    }

    public Seller(String id, String username, String password, String phone, String shopName) {
        super(id, username, password, phone);
        this.shopName = shopName;
        this.rating = 0.0;
        this.soldCount = 0;
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    @Override
    public String getMenuTitle() {
        return "卖家菜单";
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

}
