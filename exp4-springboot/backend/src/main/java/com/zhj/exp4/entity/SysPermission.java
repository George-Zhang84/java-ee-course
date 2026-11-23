package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限表实体（sys_permission）。
 *
 * <p>type 三种取值：
 * <ul>
 *   <li>MENU：前端路由/菜单项，决定侧边栏是否显示；</li>
 *   <li>BUTTON：页面内按钮级权限，前端用 v-permission 指令控制；</li>
 *   <li>API：纯接口权限，不对应界面元素。</li>
 * </ul>
 * 鉴权时只看 {@code code}（如 {@code product:audit}），与 type 无关。
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限码，全局唯一，如 product:publish */
    private String code;

    private String name;

    /** MENU / BUTTON / API */
    private String type;

    /** 父权限 id，0 表示顶级（当前为平铺结构，预留树形扩展） */
    private Long parentId;

    /** 菜单对应前端路由；按钮/接口权限可为空 */
    private String path;

    private String icon;

    private Integer sort;
}
