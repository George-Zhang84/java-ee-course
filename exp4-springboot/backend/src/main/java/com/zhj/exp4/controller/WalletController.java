package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.RechargeRequest;
import com.zhj.exp4.dto.response.WalletLogResponse;
import com.zhj.exp4.dto.response.WalletResponse;
import com.zhj.exp4.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 钱包接口。充值为演示用的模拟充值（无第三方支付渠道）。
 */
@Tag(name = "08. 钱包", description = "余额、充值、流水")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "钱包概览（余额 + 累计收支）")
    @GetMapping
    public Result<WalletResponse> info() {
        return Result.ok(walletService.info());
    }

    @Operation(summary = "模拟充值")
    @PreAuthorize("hasAuthority('wallet:recharge')")
    @Audit(module = "钱包", action = "充值")
    @PostMapping("/recharge")
    public Result<Void> recharge(@Valid @RequestBody RechargeRequest request) {
        walletService.recharge(request.getAmount());
        return Result.ok("充值成功", null);
    }

    @Operation(summary = "资金流水分页")
    @GetMapping("/logs")
    public Result<PageResult<WalletLogResponse>> logs(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(walletService.pageLogs(page, size));
    }
}
