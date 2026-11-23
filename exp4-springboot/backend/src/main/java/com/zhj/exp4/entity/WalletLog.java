package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水实体（wallet_log）。
 *
 * <p>只追加、不修改、不删除：任何余额变动都必须先改 {@code sys_user.balance}
 * 再写一条流水，两者在同一事务内，{@code balanceAfter} 用于对账校验。
 */
@Data
@TableName("wallet_log")
public class WalletLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** RECHARGE 充值 / PAY 支付 / INCOME 卖家入账 / REFUND 退款 */
    private String type;

    /** 正数收入、负数支出 */
    private BigDecimal amount;

    /** 变动后余额，便于人工核对 */
    private BigDecimal balanceAfter;

    /** 关联业务类型，如 ORDER */
    private String bizType;

    /** 关联业务 id，如订单 id */
    private Long bizId;

    private String remark;

    private LocalDateTime createTime;
}
