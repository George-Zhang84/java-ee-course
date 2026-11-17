package com.zhj.service;

import com.zhj.dao.OrderDao;
import com.zhj.dao.ProductDao;
import com.zhj.infrastructure.IdUtil;
import com.zhj.model.Order;
import com.zhj.model.Product;

import java.util.List;

public class OrderService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_RECEIVED = "RECEIVED";

    private final OrderDao orderDao = new OrderDao();
    private final ProductDao productDao = new ProductDao();

    /**
     * 下单。返回 "success" 或具体失败原因。
     * 注意：余额校验和商品抢占都放到 DAO 的事务里做，这里只做快速的前置校验。
     */
    public String createOrder(String buyerId, String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return "商品ID不能为空";
        }
        Product product = productDao.findById(productId);
        if (product == null) {
            return "商品不存在";
        }
        if (buyerId.equals(product.getSellerId())) {
            return "不能购买自己发布的商品";
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            return "商品已下架或已售出";
        }

        Order order = new Order();
        order.setId(IdUtil.next("O"));
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setAmount(product.getPrice());
        order.setStatus(STATUS_PENDING);

        return orderDao.createOrder(order);
    }

    public List<Order> getOrders(String userId) {
        return orderDao.findList(userId);
    }

    public Order getOrder(String orderId) {
        return orderDao.findById(orderId);
    }

    /**
     * 订单状态流转（服务端状态机 + 归属校验）。
     *
     * <p>原实现完全信任前端传来的 currentStatus，且只校验角色不校验订单归属，
     * 导致买家可以伪造 currentStatus 把任意订单改成已收货，卖家也能确认别人的订单。
     * 现在改为以数据库中的真实状态与归属为准。</p>
     */
    public String updateOrderStatus(String orderId, String newStatus, String operatorId, String roleCode) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return "订单号不能为空";
        }
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return "目标状态不能为空";
        }
        Order order = orderDao.findById(orderId);
        if (order == null) {
            return "订单不存在";
        }

        if (STATUS_CONFIRMED.equals(newStatus)) {
            // 卖家确认发货：PENDING -> CONFIRMED
            if (!"SELLER".equals(roleCode)) {
                return "只有卖家可以确认订单";
            }
            if (!operatorId.equals(order.getSellerId())) {
                return "无权操作他人的订单";
            }
            if (!STATUS_PENDING.equals(order.getStatus())) {
                return "当前订单状态不允许确认";
            }
            return orderDao.confirmOrder(orderId, operatorId) ? "success" : "订单状态已变更，请刷新后重试";
        }

        if (STATUS_RECEIVED.equals(newStatus)) {
            // 买家确认收货：CONFIRMED -> RECEIVED，同时给卖家结算货款
            if (!"BUYER".equals(roleCode)) {
                return "只有买家可以确认收货";
            }
            if (!operatorId.equals(order.getBuyerId())) {
                return "无权操作他人的订单";
            }
            if (!STATUS_CONFIRMED.equals(order.getStatus())) {
                return "卖家还未确认订单，不能收货";
            }
            return orderDao.completeOrder(orderId, operatorId) ? "success" : "订单状态已变更，请刷新后重试";
        }

        return "非法的订单状态";
    }
}
