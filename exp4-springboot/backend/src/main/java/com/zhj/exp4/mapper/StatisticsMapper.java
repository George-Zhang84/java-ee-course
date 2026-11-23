package com.zhj.exp4.mapper;

import com.zhj.exp4.dto.response.CategoryStat;
import com.zhj.exp4.dto.response.HotProduct;
import com.zhj.exp4.dto.response.TrendPoint;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计 Mapper（数据看板专用）。
 *
 * <p>为什么不复用 BaseMapper：这些都是聚合查询，SQL 本身就是"业务视图"，
 * 集中在一个 Mapper 里比散落到各实体 Mapper 更清晰，也便于 DBA 审阅与加索引。
 *
 * <p>GMV 口径：只统计 PAID / SHIPPED / COMPLETED 三种状态，
 * 未支付与已取消不算成交，避免看板虚高。
 */
public interface StatisticsMapper {

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    Long countUsers();

    @Select("SELECT COUNT(*) FROM product WHERE deleted = 0 AND status = 'ON_SALE'")
    Long countOnSaleProducts();

    @Select("SELECT COUNT(*) FROM product WHERE deleted = 0 AND status = 'PENDING'")
    Long countPendingProducts();

    @Select("SELECT COUNT(*) FROM `order`")
    Long countOrders();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` "
            + "WHERE status IN ('PAID', 'SHIPPED', 'COMPLETED')")
    BigDecimal sumGmv();

    @Select("SELECT COUNT(*) FROM `order` WHERE DATE(create_time) = CURDATE()")
    Long countTodayOrders();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` "
            + "WHERE DATE(create_time) = CURDATE() AND status IN ('PAID', 'SHIPPED', 'COMPLETED')")
    BigDecimal sumTodayGmv();

    /**
     * 近 N 日趋势。只返回"有订单的日期"，前端按日期补齐空档。
     *
     * @param start 起始时间（含）
     */
    @Select("SELECT DATE(create_time) AS statDate, COUNT(*) AS orderCount, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID','SHIPPED','COMPLETED') THEN amount ELSE 0 END), 0) AS gmv "
            + "FROM `order` WHERE create_time >= #{start} "
            + "GROUP BY DATE(create_time) ORDER BY statDate")
    List<TrendPoint> trendSince(@Param("start") LocalDateTime start);

    /** 分类商品数量占比 */
    @Select("SELECT c.name AS name, COUNT(p.id) AS productCount FROM category c "
            + "LEFT JOIN product p ON p.category_id = c.id AND p.deleted = 0 "
            + "GROUP BY c.id, c.name ORDER BY productCount DESC")
    List<CategoryStat> categoryStats();

    /** 热销商品 TopN：按成交笔数降序 */
    @Select("SELECT p.id AS productId, p.title AS title, COUNT(o.id) AS soldCount, "
            + "COALESCE(SUM(o.amount), 0) AS gmv FROM `order` o "
            + "JOIN product p ON p.id = o.product_id "
            + "WHERE o.status IN ('PAID', 'SHIPPED', 'COMPLETED') "
            + "GROUP BY p.id, p.title ORDER BY soldCount DESC, gmv DESC LIMIT #{limit}")
    List<HotProduct> hotProducts(@Param("limit") int limit);
}
