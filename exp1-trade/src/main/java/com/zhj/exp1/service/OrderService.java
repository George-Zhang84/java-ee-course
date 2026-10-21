package com.zhj.exp1.service;

import com.zhj.exp1.exception.BusinessException;
import com.zhj.exp1.exception.InsufficientBalanceException;
import com.zhj.exp1.exception.InvalidOperationException;
import com.zhj.exp1.model.Buyer;
import com.zhj.exp1.model.Order;
import com.zhj.exp1.model.Product;
import com.zhj.exp1.model.Seller;
import com.zhj.exp1.model.enums.OrderStatus;
import com.zhj.exp1.model.enums.ProductStatus;
import com.zhj.exp1.repository.DataStore;
import com.zhj.exp1.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单业务逻辑层
 * 处理下单、确认、收货、取消等流程
 * <p>
 * 订单状态机：
 * PENDING -> CONFIRMED -> RECEIVED   (正常流程)
 * PENDING -> CANCELLED                (买家取消或卖家拒绝)
 */
public class OrderService {

    private final DataStore store = DataStore.getInstance();
    private final ProductService productService = new ProductService();

    /**
     * 买家下单
     */
    public Order createOrder(String buyerId, String productId) {
        Buyer buyer = (Buyer) store.findUserById(buyerId);
        Product product = store.findProductById(productId);

        if (product == null) {
            throw new InvalidOperationException("商品不存在");
        }
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new InvalidOperationException("该商品当前不可购买（状态：" + product.getStatus() + "）");
        }
        if (product.getSellerId().equals(buyerId)) {
            throw new InvalidOperationException("不能购买自己发布的商品");
        }
        if (buyer.getBalance() < product.getPrice()) {
            throw new InsufficientBalanceException(
                    String.format("余额不足，需要 ￥%.2f，当前余额 ￥%.2f",
                            product.getPrice(), buyer.getBalance()));
        }

        // 扣款
        buyer.setBalance(buyer.getBalance() - product.getPrice());

        // 创建订单
        Order order = new Order(
                IdGenerator.nextOrderId(),
                buyerId,
                product.getSellerId(),
                productId
        );
        store.addOrder(order);

        // 商品锁定（改为已售出状态，防止其他人下单）
        product.setStatus(ProductStatus.SOLD);

        return order;
    }

    /**
     * 卖家确认订单
     */
    public void confirmOrder(String orderId, String sellerId) {
        Order order = getOrderOrThrow(orderId);
        checkSeller(order, sellerId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("只有【待确认】订单可以确认");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmTime(LocalDateTime.now());
    }

    /**
     * 卖家拒绝订单（退钱给买家，商品恢复在售）
     */
    public void rejectOrder(String orderId, String sellerId) {
        Order order = getOrderOrThrow(orderId);
        checkSeller(order, sellerId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("只有【待确认】订单可以拒绝");
        }

        // 退款
        Buyer buyer = (Buyer) store.findUserById(order.getBuyerId());
        Product product = store.findProductById(order.getProductId());
        buyer.setBalance(buyer.getBalance() + product.getPrice());

        // 商品恢复在售
        product.setStatus(ProductStatus.ON_SALE);

        order.setStatus(OrderStatus.CANCELLED);
    }

    /**
     * 买家取消订单（仅待确认状态）
     */
    public void cancelOrder(String orderId, String buyerId) {
        Order order = getOrderOrThrow(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new InvalidOperationException("无权操作他人的订单");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("只有【待确认】订单可以取消");
        }

        // 退款
        Buyer buyer = (Buyer) store.findUserById(buyerId);
        Product product = store.findProductById(order.getProductId());
        buyer.setBalance(buyer.getBalance() + product.getPrice());

        // 商品恢复在售
        product.setStatus(ProductStatus.ON_SALE);

        order.setStatus(OrderStatus.CANCELLED);
    }

    /**
     * 买家确认收货（卖家收到钱）
     */
    public void receiveOrder(String orderId, String buyerId) {
        Order order = getOrderOrThrow(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new InvalidOperationException("无权操作他人的订单");
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOperationException("只有【已确认】订单可以收货");
        }

        // 卖家收款
        Seller seller = (Seller) store.findUserById(order.getSellerId());
        Product product = store.findProductById(order.getProductId());
        // 因为没有卖家余额字段，这里简单记录销量
        seller.setSoldCount(seller.getSoldCount() + 1);

        order.setStatus(OrderStatus.RECEIVED);
        order.setReceiveTime(LocalDateTime.now());
    }

    // ================== 查询 ==================

    public Order getOrderOrThrow(String orderId) {
        Order order = store.findOrderById(orderId);
        if (order == null) {
            throw new BusinessException("订单 ID [" + orderId + "] 不存在");
        }
        return order;
    }

    public List<Order> listOrdersByBuyer(String buyerId) {
        return store.findOrdersByBuyer(buyerId);
    }

    public List<Order> listOrdersBySeller(String sellerId) {
        return store.findOrdersBySeller(sellerId);
    }

    public List<Order> listAllOrders() {
        return store.getAllOrders();
    }

    // ================== 内部工具 ==================

    private void checkSeller(Order order, String sellerId) {
        if (!order.getSellerId().equals(sellerId)) {
            throw new InvalidOperationException("无权操作他人的订单");
        }
    }
}