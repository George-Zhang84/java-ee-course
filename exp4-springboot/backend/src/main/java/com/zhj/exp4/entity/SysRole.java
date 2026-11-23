package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色表实体（sys_role）。系统内置 4 个角色：SUPER_ADMIN / ADMIN / USER / SELLER。
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码，Spring Security 中会映射为 ROLE_ 前缀的 authority */
    private String code;

    private String name;

    private String description;

    private LocalDateTime createTime;
}
