package com.zhj.model;

public class User {
    private String id;
    private String username;
    private String password; // 数据库中存的是密文
    private String roleCode;
    private Double balance;
    private String shopName;
    private Integer status;

    // 必须有无参构造，方便反射/JSON序列化
    public User() {}

    // Getter 和 Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}