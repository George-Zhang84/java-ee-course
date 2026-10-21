package com.zhj.exp2.service;

import com.zhj.exp2.dao.PermissionDao;
import com.zhj.exp2.model.Permission;

import java.util.List;

public class PermService {
    private final PermissionDao permDao = new PermissionDao();

    public List<Permission> getAllPermissions() {
        return permDao.findAll();
    }
}