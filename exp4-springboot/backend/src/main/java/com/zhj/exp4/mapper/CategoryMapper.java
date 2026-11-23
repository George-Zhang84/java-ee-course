package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.Category;

/**
 * 商品分类 Mapper（对应表 category）。
 *
 * <p>继承 BaseMapper 即获得单表 CRUD、条件构造器与分页能力；
 * 多表关联或聚合统计通过注解 SQL / XML 补充。
 */
public interface CategoryMapper extends BaseMapper<Category> {
}
