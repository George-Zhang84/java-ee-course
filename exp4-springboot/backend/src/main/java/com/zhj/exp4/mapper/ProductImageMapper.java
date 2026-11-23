package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.ProductImage;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品图片 Mapper（对应表 product_image）。
 *
 * <p>除 CRUD 外提供"全量 URL 查询"：孤儿图片清理任务需要知道哪些文件仍被引用。
 */
public interface ProductImageMapper extends BaseMapper<ProductImage> {

    /** 所有被商品引用的图片 URL（清理孤儿文件时做白名单） */
    @Select("SELECT url FROM product_image")
    List<String> selectAllUrls();
}
