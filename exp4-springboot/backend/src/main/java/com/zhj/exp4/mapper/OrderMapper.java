package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper（对应表 `order`）。
 *
 * <p>表名是 MySQL 保留字，SQL 中一律用反引号包裹。
 *
 * <p>这里把"状态流转"做成带 expected 条件的 UPDATE：
 * 支付、发货、收货、取消都只有一次成功机会，
 * 重复请求或并发请求会因为影响行数为 0 而被识别出来。
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 状态流转 CAS：仅当当前状态为 expected 时才更新为 target。
     *
     * @return 影响行数，0 表示状态已被别人变更
     */
    @Update("UPDATE `order` SET status = #{target} WHERE id = #{id} AND status = #{expected}")
    int casStatus(@Param("id") Long id,
                  @Param("expected") String expected,
                  @Param("target") String target);

    /** 支付：同时写入支付时间 */
    @Update("UPDATE `order` SET status = 'PAID', pay_time = #{payTime} "
            + "WHERE id = #{id} AND status = 'UNPAID'")
    int markPaid(@Param("id") Long id, @Param("payTime") LocalDateTime payTime);

    /** 发货：同时写入发货时间 */
    @Update("UPDATE `order` SET status = 'SHIPPED', ship_time = #{shipTime} "
            + "WHERE id = #{id} AND status = 'PAID'")
    int markShipped(@Param("id") Long id, @Param("shipTime") LocalDateTime shipTime);

    /** 确认收货：同时写入完成时间 */
    @Update("UPDATE `order` SET status = 'COMPLETED', finish_time = #{finishTime} "
            + "WHERE id = #{id} AND status = 'SHIPPED'")
    int markCompleted(@Param("id") Long id, @Param("finishTime") LocalDateTime finishTime);

    /** 取消：写入取消原因 */
    @Update("UPDATE `order` SET status = 'CANCELLED', cancel_reason = #{reason} "
            + "WHERE id = #{id} AND status = #{expected}")
    int markCancelled(@Param("id") Long id,
                      @Param("expected") String expected,
                      @Param("reason") String reason);

    /**
     * 查询超时未支付订单（定时任务用）。
     *
     * @param deadline 创建时间早于该时刻即视为超时
     * @param limit    单次处理上限，避免一次扫描过多导致长事务
     */
    @Select("SELECT * FROM `order` WHERE status = 'UNPAID' AND create_time < #{deadline} "
            + "ORDER BY create_time LIMIT #{limit}")
    List<Order> selectTimeoutUnpaid(@Param("deadline") LocalDateTime deadline,
                                    @Param("limit") int limit);
}
