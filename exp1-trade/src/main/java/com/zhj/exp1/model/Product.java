package com.zhj.exp1.model;

import com.zhj.exp1.model.enums.Category;
import com.zhj.exp1.model.enums.ProductStatus;

import java.time.LocalDateTime;

/**
 * 商品实体类
 * 记录一件商品的完整信息
 */
public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private Category category;
    private String sellerId;
    private ProductStatus status;
    private LocalDateTime publishTime;  // 发布时间
    private int viewCount;  // 浏览量

    public Product() {
    }

    public Product(String id, String name, String description, double price, Category category, String sellerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.sellerId = sellerId;
        status = ProductStatus.PENDING;
        publishTime = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }


    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s ￥%.2f (%s)", category, name, price, status);
    }

}
