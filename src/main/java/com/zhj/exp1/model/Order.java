package com.zhj.exp1.model;

import com.zhj.exp1.model.enums.OrderStatus;

import java.time.LocalDateTime;

/**
 * 订单实体类
 * 记录一次二手交易的完整信息
 */
public class Order {

    private String id;                  // 订单编号
    private String buyerId;             // 买家ID
    private String sellerId;            // 卖家ID
    private String productId;           // 商品ID
    private OrderStatus status;         // 订单状态
    private LocalDateTime createTime;   // 下单时间
    private LocalDateTime confirmTime;  // 卖家确认时间
    private LocalDateTime receiveTime;  // 买家收货时间

    public Order() {
    }

    public Order(String id, String buyerId, String sellerId, String productId) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.status = OrderStatus.PENDING;      // 新建订单默认待确认
        this.createTime = LocalDateTime.now();
    }

    // ========== Getter / Setter ==========
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }

    public LocalDateTime getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    @Override
    public String toString() {
        return String.format("订单[%s] 商品:%s 状态:%s 下单时间:%s",
                id, productId, status, createTime);
    }
}