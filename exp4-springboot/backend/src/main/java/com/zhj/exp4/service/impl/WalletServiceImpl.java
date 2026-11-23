package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.dto.response.WalletLogResponse;
import com.zhj.exp4.dto.response.WalletResponse;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.entity.WalletLog;
import com.zhj.exp4.enums.WalletType;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.mapper.WalletLogMapper;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 钱包服务实现。
 *
 * <p>核心不变量：
 * <ol>
 *   <li><b>余额永不为负</b>：扣款用 {@code WHERE balance >= amount} 原子判断；</li>
 *   <li><b>账实相符</b>：每次余额变动都写一条流水，并记录变动后余额；</li>
 *   <li><b>同事务</b>：余额更新与流水写入在同一个事务里，不会出现"只扣钱不记账"。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final SysUserMapper userMapper;
    private final WalletLogMapper walletLogMapper;

    @Override
    public WalletResponse info() {
        Long userId = SecurityUtils.currentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return WalletResponse.builder()
                .balance(user.getBalance())
                .totalIncome(walletLogMapper.sumIncome(userId))
                .totalExpense(walletLogMapper.sumExpense(userId))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(BigDecimal amount) {
        Long userId = SecurityUtils.currentUserId();
        int rows = userMapper.addBalance(userId, amount);
        if (rows == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        writeLog(userId, WalletType.RECHARGE, amount, "RECHARGE", null, "钱包充值");
        log.info("用户 {} 充值 {} 元", userId, amount);
    }

    @Override
    public PageResult<WalletLogResponse> pageLogs(int page, int size) {
        Long userId = SecurityUtils.currentUserId();
        Page<WalletLog> result = walletLogMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                Wrappers.<WalletLog>lambdaQuery()
                        .eq(WalletLog::getUserId, userId)
                        .orderByDesc(WalletLog::getCreateTime));
        return PageResult.of(result, this::toResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long userId, BigDecimal amount, WalletType type,
                       String bizType, Long bizId, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.of("扣款金额必须大于 0");
        }
        int rows = userMapper.deductBalance(userId, amount);
        if (rows == 0) {
            // 余额不足或用户不存在，两者对调用方都是"钱不够"
            throw new BizException(ResultCode.BALANCE_NOT_ENOUGH);
        }
        writeLog(userId, type, amount.negate(), bizType, bizId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void income(Long userId, BigDecimal amount, WalletType type,
                       String bizType, Long bizId, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.of("入账金额必须大于 0");
        }
        int rows = userMapper.addBalance(userId, amount);
        if (rows == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "收款用户不存在");
        }
        writeLog(userId, type, amount, bizType, bizId, remark);
    }

    /** 写流水：amount 正负决定收支方向，balanceAfter 取当前余额 */
    private void writeLog(Long userId, WalletType type, BigDecimal amount,
                          String bizType, Long bizId, String remark) {
        SysUser user = userMapper.selectById(userId);
        WalletLog logEntity = new WalletLog();
        logEntity.setUserId(userId);
        logEntity.setType(type.name());
        logEntity.setAmount(amount);
        logEntity.setBalanceAfter(user == null ? BigDecimal.ZERO : user.getBalance());
        logEntity.setBizType(bizType);
        logEntity.setBizId(bizId);
        logEntity.setRemark(remark);
        walletLogMapper.insert(logEntity);
    }

    private WalletLogResponse toResponse(WalletLog logEntity) {
        String typeText;
        try {
            typeText = WalletType.valueOf(logEntity.getType()).getDescription();
        } catch (Exception e) {
            typeText = logEntity.getType();
        }
        return WalletLogResponse.builder()
                .id(logEntity.getId())
                .type(logEntity.getType())
                .typeText(typeText)
                .amount(logEntity.getAmount())
                .balanceAfter(logEntity.getBalanceAfter())
                .bizType(logEntity.getBizType())
                .bizId(logEntity.getBizId())
                .remark(logEntity.getRemark())
                .createTime(logEntity.getCreateTime())
                .build();
    }
}
