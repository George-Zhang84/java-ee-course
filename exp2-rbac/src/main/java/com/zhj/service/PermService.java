package com.zhj.service;

import com.zhj.dao.PermissionDao;
import com.zhj.model.Permission;

import java.util.List;

public class PermService {
    private final PermissionDao permDao = new PermissionDao();

    public List<Permission> getAllPermissions() {
        return permDao.findAll();
    }
}