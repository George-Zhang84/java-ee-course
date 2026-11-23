package com.zhj.exp4.service;

import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.entity.WalletLog;
import com.zhj.exp4.enums.WalletType;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.mapper.WalletLogMapper;
import com.zhj.exp4.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 钱包服务单元测试（Mockito，不连数据库）。
 *
 * <p>重点验证两条不变量：
 * <ol>
 *   <li>余额不足时扣款失败，且**不写流水**（不能出现"记账了但没扣钱"）；</li>
 *   <li>扣款成功后必须写一条负数流水，金额与类型正确。</li>
 * </ol>
 */
@DisplayName("钱包扣款与记账")
class WalletServiceImplTest {

    private SysUserMapper userMapper;
    private WalletLogMapper walletLogMapper;
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        userMapper = Mockito.mock(SysUserMapper.class);
        walletLogMapper = Mockito.mock(WalletLogMapper.class);
        walletService = new WalletServiceImpl(userMapper, walletLogMapper);
    }

    @Test
    @DisplayName("余额充足：扣款成功并写一条支出流水")
    void deductSuccess() {
        when(userMapper.deductBalance(eq(3L), any())).thenReturn(1);
        when(userMapper.selectById(3L)).thenReturn(user(3L, "1185.00"));

        walletService.deduct(3L, new BigDecimal("15.00"), WalletType.PAY, "ORDER", 9L, "支付订单");

        ArgumentCaptor<WalletLog> captor = ArgumentCaptor.forClass(WalletLog.class);
        verify(walletLogMapper).insert(captor.capture());
        WalletLog saved = captor.getValue();
        assertEquals("PAY", saved.getType());
        assertEquals(0, new BigDecimal("-15.00").compareTo(saved.getAmount()));
        assertEquals(0, new BigDecimal("1185.00").compareTo(saved.getBalanceAfter()));
        assertEquals(9L, saved.getBizId());
    }

    @Test
    @DisplayName("余额不足：抛业务异常且不写流水")
    void deductInsufficientBalance() {
        when(userMapper.deductBalance(eq(3L), any())).thenReturn(0);

        BizException ex = assertThrows(BizException.class, () ->
                walletService.deduct(3L, new BigDecimal("9999.00"), WalletType.PAY, "ORDER", 9L, "支付订单"));

        assertEquals(ResultCode.BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
        verify(walletLogMapper, never()).insert(any(WalletLog.class));
    }

    @Test
    @DisplayName("金额非法（0 或负数）直接拒绝")
    void rejectNonPositiveAmount() {
        assertThrows(BizException.class, () ->
                walletService.deduct(3L, BigDecimal.ZERO, WalletType.PAY, "ORDER", 9L, "x"));
        assertThrows(BizException.class, () ->
                walletService.income(5L, new BigDecimal("-1"), WalletType.INCOME, "ORDER", 9L, "x"));
        verify(walletLogMapper, never()).insert(any(WalletLog.class));
    }

    @Test
    @DisplayName("入账成功：写入正数流水")
    void incomeSuccess() {
        when(userMapper.addBalance(eq(5L), any())).thenReturn(1);
        when(userMapper.selectById(5L)).thenReturn(user(5L, "140.00"));

        walletService.income(5L, new BigDecimal("120.00"), WalletType.INCOME, "ORDER", 9L, "结算入账");

        ArgumentCaptor<WalletLog> captor = ArgumentCaptor.forClass(WalletLog.class);
        verify(walletLogMapper).insert(captor.capture());
        assertEquals("INCOME", captor.getValue().getType());
        assertEquals(0, new BigDecimal("120.00").compareTo(captor.getValue().getAmount()));
    }

    private SysUser user(Long id, String balance) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setBalance(new BigDecimal(balance));
        return user;
    }
}
