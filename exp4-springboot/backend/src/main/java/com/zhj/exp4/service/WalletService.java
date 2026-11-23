package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.response.WalletLogResponse;
import com.zhj.exp4.dto.response.WalletResponse;
import com.zhj.exp4.enums.WalletType;

import java.math.BigDecimal;

/**
 * 钱包服务。
 *
 * <p>所有余额变动都必须经过本服务，且在调用方的事务内执行（PROPAGATION_REQUIRED）：
 * 业务操作与记账要么一起成功，要么一起回滚，绝不允许"钱扣了但订单没变"。
 */
public interface WalletService {

    /** 当前用户钱包概览 */
    WalletResponse info();

    /** 模拟充值 */
    void recharge(BigDecimal amount);

    /** 流水分页 */
    PageResult<WalletLogResponse> pageLogs(int page, int size);

    /**
     * 扣款并记账。
     *
     * @param userId  用户 id
     * @param amount  金额（正数）
     * @param type    流水类型
     * @param bizType 关联业务类型
     * @param bizId   关联业务 id
     * @param remark  备注
     * @throws com.zhj.exp4.common.BizException 余额不足时抛出
     */
    void deduct(Long userId, BigDecimal amount, WalletType type,
                String bizType, Long bizId, String remark);

    /**
     * 入账并记账。
     *
     * @param userId  用户 id
     * @param amount  金额（正数）
     * @param type    流水类型
     * @param bizType 关联业务类型
     * @param bizId   关联业务 id
     * @param remark  备注
     */
    void income(Long userId, BigDecimal amount, WalletType type,
                String bizType, Long bizId, String remark);
}
