package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.SysRolePermission;

/**
 * 角色-权限关联 Mapper（对应表 sys_role_permission）。
 *
 * <p>继承 BaseMapper 即获得单表 CRUD、条件构造器与分页能力；
 * 多表关联或聚合统计通过注解 SQL / XML 补充。
 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
}
