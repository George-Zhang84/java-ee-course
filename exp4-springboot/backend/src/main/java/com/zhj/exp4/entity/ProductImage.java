package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品图片实体（product_image）。一个商品最多 5 张图，sort 决定展示顺序，
 * 第一张同时写入 product.cover_url 作为封面，列表页只取封面避免多表 join。
 */
@Data
@TableName("product_image")
public class ProductImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 相对 URL，如 /uploads/2025/12/xxx.jpg */
    private String url;

    private Integer sort;
}
