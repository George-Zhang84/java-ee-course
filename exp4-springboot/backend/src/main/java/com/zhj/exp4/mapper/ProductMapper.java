package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品 Mapper（对应表 product）。
 *
 * <p>除 BaseMapper 的 CRUD 外，提供两个"条件更新（CAS）"方法：
 * 它们把"状态判断 + 写入"合并成一条原子 SQL，是防止一物两卖的关键。
 * 如果用"先 select 判断状态、再 update"的写法，两个并发请求可能同时通过判断。
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 原子地把商品从 expected 状态改为 target 状态。
     *
     * @return 影响行数；0 表示状态已被别人改掉（抢占失败）
     */
    @Update("UPDATE product SET status = #{target} WHERE id = #{id} AND status = #{expected} AND deleted = 0")
    int casStatus(@Param("id") Long id,
                  @Param("expected") String expected,
                  @Param("target") String target);

    /**
     * 所有商品封面 URL（孤儿图片清理时做引用白名单）。
     */
    @Select("SELECT cover_url FROM product WHERE cover_url IS NOT NULL AND cover_url <> ''")
    List<String> selectAllCovers();

    /**
     * 浏览量自增（用 SQL 表达式，避免读-改-写丢计数）。
     */
    @Update("UPDATE product SET views = views + 1 WHERE id = #{id}")
    int increaseViews(@Param("id") Long id);
}
