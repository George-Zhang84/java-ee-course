package com.zhj.service;

import com.zhj.dao.RoleDao;
import com.zhj.dao.UserDao;
import com.zhj.infrastructure.exception.BusinessException;
import com.zhj.infrastructure.MD5Util;
import com.zhj.model.User;

import java.util.List;

public class UserService {
    private final UserDao userDao = new UserDao();
    private final RoleDao roleDao = new RoleDao();

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    /**
     * 获取用户的角色编码
     */
    public String getUserRoleCode(User user) {
        return userDao.getRoleCodeByUserId(user.getId());
    }

    /**
     * 删除用户 (包含防自杀与越权校验)
     */
    public boolean deleteUser(User operator, int targetId) {
        User target = userDao.findAll().stream()
                .filter(u -> u.getId() == targetId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("目标用户不存在"));

        if (operator.getId() == targetId) {
            throw new BusinessException("安全限制：不能删除当前登录的自己！");
        }
        if (target.getUsername().equals("admin")) {
            throw new BusinessException("安全限制：不能删除超级管理员 admin！");
        }

        String opRole = userDao.getRoleCodeByUserId(operator.getId());
        String targetRole = userDao.getRoleCodeByUserId(targetId);
        if (!"SUPER_ADMIN".equals(opRole) && ("SUPER_ADMIN".equals(targetRole) || "USER_MANAGER".equals(targetRole))) {
            throw new BusinessException("安全拦截：你的权限不足以删除 [" + targetRole + "] 级别的账号！");
        }

        return userDao.deleteById(targetId);
    }

    /**
     * 创建用户 (包含越权创建校验)
     */
    public boolean createUser(User operator, String username, String rawPassword, int roleId) {
        if (userDao.existsByUsername(username)) {
            throw new BusinessException("创建失败：用户名已存在");
        }

        String operatorRole = userDao.getRoleCodeByUserId(operator.getId());
        String targetRole = roleDao.getRoleCodeById(roleId);

        if (targetRole == null) {
            throw new BusinessException("创建失败：选择的角色不存在");
        }

        if (!"SUPER_ADMIN".equals(operatorRole)) {
            if ("SUPER_ADMIN".equals(targetRole) || "USER_MANAGER".equals(targetRole)) {
                throw new BusinessException("安全拦截：你的权限不足以创建 [" + targetRole + "] 级别的账号！");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(MD5Util.encrypt(rawPassword));
        user.setStatus(1);
        return userDao.insert(user, roleId);
    }

    /**
     * 更新用户状态 (包含防篡改 admin 逻辑)
     */
    public boolean updateUserStatus(User operator, int targetId, int newStatus) {
        User target = userDao.findAll().stream()
                .filter(u -> u.getId() == targetId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("目标用户不存在"));

        if (target.getUsername().equals("admin")) {
            throw new BusinessException("安全限制：不能修改超级管理员 admin 的状态");
        }

        return userDao.updateStatus(targetId, newStatus);
    }
}