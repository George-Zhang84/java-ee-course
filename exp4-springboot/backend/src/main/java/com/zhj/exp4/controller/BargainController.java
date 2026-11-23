package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.OfferCreateRequest;
import com.zhj.exp4.dto.response.OfferCreateResponse;
import com.zhj.exp4.dto.response.OfferResponse;
import com.zhj.exp4.service.BargainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 议价接口（特色功能 S1）。
 *
 * <p>接受报价会直接生成订单，因此返回订单 id，前端可跳转到"待支付"页。
 */
@Tag(name = "07. 议价", description = "出价、接受、拒绝、撤回、报价列表")
@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class BargainController {

    private final BargainService bargainService;

    @Operation(summary = "买家出价（达到卖家底价时自动成交）")
    @PreAuthorize("hasAuthority('bargain:offer')")
    @Audit(module = "议价", action = "发起还价")
    @PostMapping
    public Result<OfferCreateResponse> create(@Valid @RequestBody OfferCreateRequest request) {
        OfferCreateResponse response = bargainService.createOffer(request);
        return Result.ok(response.getMessage(), response);
    }

    @Operation(summary = "我发出的报价")
    @GetMapping("/sent")
    public Result<PageResult<OfferResponse>> sent(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(bargainService.pageSent(page, size));
    }

    @Operation(summary = "我收到的报价")
    @GetMapping("/received")
    public Result<PageResult<OfferResponse>> received(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(bargainService.pageReceived(page, size));
    }

    @Operation(summary = "卖家接受报价（锁定商品并生成订单）")
    @PreAuthorize("hasAuthority('bargain:handle')")
    @Audit(module = "议价", action = "接受还价")
    @PostMapping("/{id}/accept")
    public Result<Long> accept(@PathVariable Long id) {
        return Result.ok("已接受报价，等待买家支付", bargainService.accept(id));
    }

    @Operation(summary = "卖家拒绝报价")
    @PreAuthorize("hasAuthority('bargain:handle')")
    @Audit(module = "议价", action = "拒绝还价")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        bargainService.reject(id);
        return Result.ok("已拒绝该报价", null);
    }

    @Operation(summary = "买家撤回报价")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        bargainService.cancel(id);
        return Result.ok("已撤回报价", null);
    }
}
