package com.zhj.exp2.service;

import com.zhj.exp2.dao.RoleDao;
import com.zhj.exp2.model.Role;

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