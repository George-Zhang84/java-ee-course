package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.BargainOffer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 议价报价单 Mapper（对应表 bargain_offer）。
 *
 * <p>接受/拒绝/撤回都要求当前状态为 PENDING，
 * 因此用带条件的 UPDATE 而不是"查出实体再改"：
 * 两个并发的"接受"只有一个能成功，第二个影响行数为 0，直接被拒绝。
 */
public interface BargainOfferMapper extends BaseMapper<BargainOffer> {

    /**
     * 状态流转 CAS。
     *
     * @return 影响行数，0 表示该报价单已被处理或已过期
     */
    @Update("UPDATE bargain_offer SET status = #{target}, handled_time = #{handledTime} "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int markHandled(@Param("id") Long id,
                    @Param("target") String target,
                    @Param("handledTime") LocalDateTime handledTime);

    /** 已过期的待处理报价单（定时任务用） */
    @Select("SELECT * FROM bargain_offer WHERE status = 'PENDING' AND expire_time < #{now} "
            + "ORDER BY expire_time LIMIT #{limit}")
    List<BargainOffer> selectExpired(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 某买家对某商品在"待处理"状态的报价数量（用于限制重复出价） */
    @Select("SELECT COUNT(*) FROM bargain_offer WHERE product_id = #{productId} "
            + "AND buyer_id = #{buyerId} AND status = 'PENDING'")
    int countPending(@Param("productId") Long productId, @Param("buyerId") Long buyerId);

    /** 某买家对某商品累计出价次数（含已结束的，用于次数上限） */
    @Select("SELECT COUNT(*) FROM bargain_offer WHERE product_id = #{productId} AND buyer_id = #{buyerId}")
    int countTotal(@Param("productId") Long productId, @Param("buyerId") Long buyerId);

    /** 订单超时取消时，把对应报价单作废 */
    @Update("UPDATE bargain_offer SET status = 'EXPIRED' WHERE id = #{id} AND status = 'ACCEPTED'")
    int expireAccepted(@Param("id") Long id);
}
