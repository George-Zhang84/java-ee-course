package com.zhj.service;

import com.zhj.dao.UserDao;
import com.zhj.infrastructure.IdUtil;
import com.zhj.infrastructure.MD5Util;
import com.zhj.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String ROLE_BUYER = "BUYER";
    private static final String ROLE_SELLER = "SELLER";

    private final UserDao userDao = new UserDao();

    /**
     * 登录校验。成功返回 User 对象，失败返回 null。
     */
    public User login(String username, String rawPassword) {
        if (username == null || username.trim().isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }
        User user = userDao.findByUsername(username.trim());
        if (user == null) {
            return null; // 用户不存在
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return null; // 账户被禁用
        }

        // 将前端传来的明文密码加密后与数据库比对
        String encryptedPwd = MD5Util.encrypt(rawPassword);
        if (encryptedPwd.equals(user.getPassword())) {
            logger.info("用户登录成功: [{}], 角色: [{}]", username, user.getRoleCode());
            return user;
        }
        return null;
    }

    /**
     * 注册。返回 "success" 或具体的中文失败原因。
     *
     * <p>关键修复：roleCode 只允许 BUYER/SELLER，绝不能信任前端传值，
     * 否则任何人都可以直接注册成 ADMIN / SUPER_ADMIN（垂直提权）。</p>
     */
    public String register(String username, String rawPassword, String roleCode, String shopName) {
        String name = username == null ? "" : username.trim();
        if (name.length() < 3 || name.length() > 20) {
            return "用户名长度需为 3-20 个字符";
        }
        if (!name.matches("[A-Za-z0-9_\\u4e00-\\u9fa5]+")) {
            return "用户名只能包含字母、数字、下划线或中文";
        }
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 32) {
            return "密码长度需为 6-32 个字符";
        }
        if (!ROLE_BUYER.equals(roleCode) && !ROLE_SELLER.equals(roleCode)) {
            return "注册角色不合法";
        }
        if (userDao.findByUsername(name) != null) {
            return "用户名已存在，请更换";
        }

        User user = new User();
        user.setId(IdUtil.next("U"));
        user.setUsername(name);
        user.setPassword(MD5Util.encrypt(rawPassword));
        user.setRoleCode(roleCode);
        user.setStatus(1);

        // 如果是买家，送1000块初始资金用于测试；如果是卖家，记录店铺名
        if (ROLE_BUYER.equals(roleCode)) {
            user.setBalance(1000.0);
            user.setShopName(null);
        } else {
            user.setBalance(0.0);
            String shop = shopName == null ? "" : shopName.trim();
            if (shop.isEmpty()) {
                shop = name + "的小店";
            }
            if (shop.length() > 100) {
                return "店铺名称不能超过 100 个字符";
            }
            user.setShopName(shop);
        }

        return userDao.insert(user) ? "success" : "注册失败，请稍后重试";
    }
}
