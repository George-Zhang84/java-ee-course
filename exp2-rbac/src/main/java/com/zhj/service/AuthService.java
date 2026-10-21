package com.zhj.service;

import com.zhj.dao.UserDao;
import com.zhj.infrastructure.MD5Util;
import com.zhj.infrastructure.exception.AuthException;
import com.zhj.model.User;

public class AuthService {

    private final UserDao userDao = new UserDao();

    /**
     * 登录校验
     *
     * @return 登录成功的用户对象
     * @throws AuthException 包含具体的失败原因（用户不存在/密码错误/账号禁用）
     */
    public User login(String username, String rawPassword) {
        User user = userDao.findByUsername(username);

        if (user == null) {
            throw new AuthException("登录失败：用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new AuthException("登录失败：账号已被禁用");
        }

        String encryptedInput = MD5Util.encrypt(rawPassword);
        if (!user.getPassword().equals(encryptedInput)) {
            throw new AuthException("登录失败：密码错误");
        }

        return user;
    }

    /**
     * 判断用户是否拥有某个权限
     */
    public boolean hasPermission(User user, String requiredPerm) {
        if (user == null) return false;

        // 为了演示简单，这里每次直接查数据库。实际开发中可缓存到内存或Session中。
        java.util.Set<String> userPerms = userDao.findPermissionsByUserId(user.getId());
        return userPerms.contains(requiredPerm);
    }
}