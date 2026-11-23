package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关联（sys_user_role）。
 *
 * <p>多对多设计：同一个账号可以既是 USER 又是 SELLER，
 * 对应"学生既能买也能卖"的真实校园场景（实验二、三的单角色字段在此升级）。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;
}
