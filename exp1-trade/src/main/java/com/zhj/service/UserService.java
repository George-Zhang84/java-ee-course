package com.zhj.service;

import com.zhj.exception.InvalidOperationException;
import com.zhj.exception.UserNotFoundException;
import com.zhj.model.Buyer;
import com.zhj.model.Seller;
import com.zhj.model.User;
import com.zhj.model.enums.UserRole;
import com.zhj.model.enums.UserStatus;
import com.zhj.repository.DataStore;
import com.zhj.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务逻辑层
 * 负责注册、登录、用户管理等功能
 */
public class UserService {

    private final DataStore store = DataStore.getInstance();

    // ================== 注册 ==================

    /**
     * 注册买家
     */
    public Buyer registerBuyer(String username, String password, String phone) {
        checkUsernameAvailable(username);
        Buyer buyer = new Buyer(IdGenerator.nextUserId(), username, password, phone);
        buyer.setBalance(1000.0);   // 新用户送 1000 元体验金
        store.addUser(buyer);
        return buyer;
    }

    /**
     * 注册卖家
     */
    public Seller registerSeller(String username, String password, String phone, String shopName) {
        checkUsernameAvailable(username);
        Seller seller = new Seller(IdGenerator.nextUserId(), username, password, phone, shopName);
        store.addUser(seller);
        return seller;
    }

    /**
     * 检查用户名是否被占用
     */
    private void checkUsernameAvailable(String username) {
        if (store.findUserByUsername(username) != null) {
            throw new InvalidOperationException("用户名 [" + username + "] 已被占用");
        }
    }

    // ================== 登录 ==================

    /**
     * 登录
     *
     * @return 登录成功的用户对象
     * @throws UserNotFoundException     用户不存在
     * @throws InvalidOperationException 密码错误或账号被禁用
     */
    public User login(String username, String password) {
        User user = store.findUserByUsername(username);
        if (user == null) {
            throw new UserNotFoundException("用户 [" + username + "] 不存在");
        }
        if (!user.getPassword().equals(password)) {
            throw new InvalidOperationException("密码错误");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidOperationException("账号已被禁用，请联系管理员");
        }
        return user;
    }

    // ================== 管理员操作 ==================

    /**
     * 禁用用户
     */
    public void disableUser(String userId) {
        User user = getUserOrThrow(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new InvalidOperationException("不允许禁用管理员账号");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new InvalidOperationException("该用户已被禁用");
        }
        user.setStatus(UserStatus.DISABLED);
    }

    /**
     * 启用用户
     */
    public void enableUser(String userId) {
        User user = getUserOrThrow(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new InvalidOperationException("该用户当前已是启用状态");
        }
        user.setStatus(UserStatus.ACTIVE);
    }

    /**
     * 重置密码为默认值 123456
     */
    public void resetPassword(String userId) {
        User user = getUserOrThrow(userId);
        user.setPassword("123456");
    }

    // ================== 查询 ==================

    public User getUserById(String userId) {
        return getUserOrThrow(userId);
    }

    /**
     * 按角色筛选用户
     */
    public List<User> listUsersByRole(UserRole role) {
        return store.getAllUsers().stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有用户
     */
    public List<User> listAllUsers() {
        return store.getAllUsers();
    }

    // ================== 私有工具方法 ==================

    /**
     * 根据 ID 获取用户，找不到则抛异常
     */
    private User getUserOrThrow(String userId) {
        User user = store.findUserById(userId);
        if (user == null) {
            throw new UserNotFoundException("用户 ID [" + userId + "] 不存在");
        }
        return user;
    }
}