package com.zhj.exp4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhj.exp4.entity.WalletLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 资金流水 Mapper（对应表 wallet_log）。
 *
 * <p>汇总统计放在 SQL 里做（SUM），而不是把流水全查出来在 Java 里累加：
 * 流水会随时间无限增长，全量加载是典型的性能陷阱。
 */
public interface WalletLogMapper extends BaseMapper<WalletLog> {

    /** 累计收入（正数流水之和） */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM wallet_log WHERE user_id = #{userId} AND amount > 0")
    BigDecimal sumIncome(@Param("userId") Long userId);

    /** 累计支出（负数流水取绝对值） */
    @Select("SELECT COALESCE(SUM(-amount), 0) FROM wallet_log WHERE user_id = #{userId} AND amount < 0")
    BigDecimal sumExpense(@Param("userId") Long userId);
}
