package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper（对应表 sys_role）。
 *
 * <p>除单表 CRUD 外，提供"按用户查角色"的关联查询：
 * 一个用户可以同时拥有 USER 与 SELLER 两个角色（多对多）。
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询某用户拥有的全部角色。
     *
     * @param userId 用户 id
     * @return 角色列表，无角色时返回空集合
     */
    @Select("SELECT r.* FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} "
            + "ORDER BY r.id")
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
