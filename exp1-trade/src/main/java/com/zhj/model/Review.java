package com.zhj.model;

import java.time.LocalDateTime;

/**
 * 评价实体类
 * 买家对已完成订单卖家的评价
 */
public class Review {

    private String id;                  // 评价ID
    private String orderId;             // 关联的订单ID
    private String buyerId;             // 评价人（买家）ID
    private String sellerId;            // 被评价的卖家ID
    private int rating;                 // 评分 1-5 星
    private String content;             // 评价文字内容
    private LocalDateTime createTime;   // 评价时间

    public Review() {
    }

    public Review(String id, String orderId, String buyerId, String sellerId,
                  int rating, String content) {
        this.id = id;
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.rating = rating;
        this.content = content;
        this.createTime = LocalDateTime.now();
    }

    // ========== Getter / Setter ==========
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return String.format("评价[%s] %d星 %s (%s)",
                orderId, rating, content, createTime.toLocalDate());
    }
}