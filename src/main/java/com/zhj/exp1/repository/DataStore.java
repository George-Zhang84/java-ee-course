package com.zhj.exp1.repository;

import com.zhj.exp1.model.Order;
import com.zhj.exp1.model.Product;
import com.zhj.exp1.model.Review;
import com.zhj.exp1.model.User;
import com.zhj.exp1.model.enums.ProductStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局数据仓库（单例模式）
 * 所有业务数据都保存在这里，service 层通过它读写数据
 */
public class DataStore {

    // ========== 单例实现 ==========
    private static DataStore instance;

    private DataStore() {
        // 私有构造，禁止外部 new
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // ========== 数据集合（Map 方便按 ID 快速查找）==========
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<String, Review> reviews = new HashMap<>();

    // ================== 用户相关 ==================
    public void addUser(User user) {
        users.put(user.getId(), user);
    }

    public User findUserById(String id) {
        return users.get(id);
    }

    /**
     * 按用户名查找（登录时用）
     */
    public User findUserByUsername(String username) {
        for (User u : users.values()) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    /**
     * 获取所有用户（返回拷贝，防止外部修改影响原集合）
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public boolean removeUser(String id) {
        return users.remove(id) != null;
    }

    public int getUserCount() {
        return users.size();
    }

    // ================== 商品相关 ==================
    public void addProduct(Product product) {
        products.put(product.getId(), product);
    }

    public Product findProductById(String id) {
        return products.get(id);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    /**
     * 按卖家ID查商品
     */
    public List<Product> findProductsBySeller(String sellerId) {
        return products.values().stream()
                .filter(p -> p.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    /**
     * 按状态查商品
     */
    public List<Product> findProductsByStatus(ProductStatus status) {
        return products.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 按关键词搜索商品名（模糊匹配，忽略大小写）
     */
    public List<Product> searchProductsByName(String keyword) {
        String kw = keyword.toLowerCase();
        return products.values().stream()
                .filter(p -> p.getName().toLowerCase().contains(kw))
                .collect(Collectors.toList());
    }

    public boolean removeProduct(String id) {
        return products.remove(id) != null;
    }

    public int getProductCount() {
        return products.size();
    }

    // ================== 订单相关 ==================
    public void addOrder(Order order) {
        orders.put(order.getId(), order);
    }

    public Order findOrderById(String id) {
        return orders.get(id);
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public List<Order> findOrdersByBuyer(String buyerId) {
        return orders.values().stream()
                .filter(o -> o.getBuyerId().equals(buyerId))
                .collect(Collectors.toList());
    }

    public List<Order> findOrdersBySeller(String sellerId) {
        return orders.values().stream()
                .filter(o -> o.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    public boolean removeOrder(String id) {
        return orders.remove(id) != null;
    }

    public int getOrderCount() {
        return orders.size();
    }

    // ================== 评价相关 ==================
    public void addReview(Review review) {
        reviews.put(review.getId(), review);
    }

    public Review findReviewById(String id) {
        return reviews.get(id);
    }

    public List<Review> getAllReviews() {
        return new ArrayList<>(reviews.values());
    }

    public List<Review> findReviewsBySeller(String sellerId) {
        return reviews.values().stream()
                .filter(r -> r.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    public List<Review> findReviewsByBuyer(String buyerId) {
        return reviews.values().stream()
                .filter(r -> r.getBuyerId().equals(buyerId))
                .collect(Collectors.toList());
    }

    public boolean removeReview(String id) {
        return reviews.remove(id) != null;
    }

    public int getReviewCount() {
        return reviews.size();
    }
}