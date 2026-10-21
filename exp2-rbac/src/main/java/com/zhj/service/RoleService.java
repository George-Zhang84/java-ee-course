package com.zhj.service;

import com.zhj.dao.RoleDao;
import com.zhj.model.Role;

import java.util.List;

public class RoleService {
    private final RoleDao roleDao = new RoleDao();

    public List<Role> getAllRoles() {
        return roleDao.findAll();
    }

    public void assignPermissions(int roleId, List<Integer> permIds) {
        roleDao.assignPermissions(roleId, permIds);
    }
}