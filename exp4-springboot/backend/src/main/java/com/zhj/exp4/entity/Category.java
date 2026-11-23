package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品分类实体（category）。分类由管理员维护，商品发布时只能选择启用状态的分类。
 */
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 前端图标名（Element Plus 图标组件名） */
    private String icon;

    private Integer sort;

    /** 1 启用 / 0 停用 */
    private Integer status;
}
