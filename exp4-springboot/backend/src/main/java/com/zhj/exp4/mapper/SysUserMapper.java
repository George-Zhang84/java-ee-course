package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户 Mapper（对应表 sys_user）。
 *
 * <p>钱包扣款/入账用"余额条件更新"实现：
 * {@code WHERE balance >= amount} 让数据库在 UPDATE 时原子判断余额是否充足，
 * 从根上杜绝"先查余额再扣款"导致的并发超扣与负余额（实验三修过的漏洞）。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 扣减余额，余额不足时不更新。
     *
     * @return 影响行数，0 表示余额不足
     */
    @Update("UPDATE sys_user SET balance = balance - #{amount} "
            + "WHERE id = #{userId} AND balance >= #{amount} AND deleted = 0")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /** 增加余额（充值/卖家入账） */
    @Update("UPDATE sys_user SET balance = balance + #{amount} WHERE id = #{userId} AND deleted = 0")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 信用分调整。
     *
     * <p>规则：初始 100 分，好评（4~5 星）+1，差评（1~2 星）-2，区间限制在 0~150。
     * 上限 150 是为了让"信用"保持可解释的区间——100 是基准，高于 100 表示历史好评多，
     * 而不是无限膨胀。
     */
    @Update("UPDATE sys_user SET credit_score = LEAST(150, GREATEST(0, credit_score + #{delta})) "
            + "WHERE id = #{userId}")
    int adjustCreditScore(@Param("userId") Long userId, @Param("delta") int delta);
}
