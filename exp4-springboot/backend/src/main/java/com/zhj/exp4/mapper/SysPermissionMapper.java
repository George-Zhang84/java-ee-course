package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.SysPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper（对应表 sys_permission）。
 *
 * <p>权限装配链路：sys_user -> sys_user_role -> sys_role_permission -> sys_permission，
 * 使用 {@code DISTINCT} 去重，避免多角色命中同一权限码时重复。
 */
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询某用户通过角色链路获得的全部权限点。
     *
     * @param userId 用户 id
     * @return 权限列表，按 sort 排序（前端菜单渲染依赖该顺序）
     */
    @Select("SELECT DISTINCT p.* FROM sys_permission p "
            + "JOIN sys_role_permission rp ON rp.permission_id = p.id "
            + "JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId} "
            + "ORDER BY p.sort")
    List<SysPermission> selectByUserId(@Param("userId") Long userId);
}
