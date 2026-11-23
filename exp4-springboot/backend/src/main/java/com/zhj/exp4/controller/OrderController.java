package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.OrderCreateRequest;
import com.zhj.exp4.dto.request.OrderQuery;
import com.zhj.exp4.dto.response.OrderResponse;
import com.zhj.exp4.service.OrderService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 *
 * <p>支付/发货/收货/取消都带 {@code @PreAuthorize} 的功能权限，
 * 参与方身份（买家还是卖家）由 Service 判断；
 * 两层校验缺一不可——只有功能权限不能防"卖家付自己的单"，只有身份校验又拦不住禁用角色。
 */
@Tag(name = "06. 订单", description = "下单、支付、发货、收货、取消、查询")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "直接购买（生成待支付订单并锁定商品）")
    @Audit(module = "订单", action = "下单")
    @PostMapping
    public Result<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.ok("下单成功，请尽快支付", orderService.create(request.getProductId()));
    }

    @Operation(summary = "我的订单（role=buyer 我买到的 / seller 我卖出的）")
    @GetMapping
    public Result<PageResult<OrderResponse>> list(OrderQuery query) {
        return Result.ok(orderService.page(query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderResponse> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @Operation(summary = "支付（钱包扣款并结算给卖家）")
    @PreAuthorize("hasAuthority('order:pay')")
    @Audit(module = "订单", action = "支付订单")
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.ok("支付成功", null);
    }

    @Operation(summary = "卖家发货")
    @PreAuthorize("hasAuthority('order:ship')")
    @Audit(module = "订单", action = "订单发货")
    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        orderService.ship(id);
        return Result.ok("发货成功", null);
    }

    @Operation(summary = "买家确认收货")
    @PreAuthorize("hasAuthority('order:confirm')")
    @Audit(module = "订单", action = "确认收货")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return Result.ok("确认收货成功，交易完成", null);
    }

    @Operation(summary = "取消订单（仅待支付）")
    @PreAuthorize("hasAuthority('order:cancel')")
    @Audit(module = "订单", action = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.ok("订单已取消", null);
    }
}
