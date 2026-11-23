package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.ProductAuditRequest;
import com.zhj.exp4.dto.response.ProductListResponse;
import com.zhj.exp4.service.AdminService;
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
 * 管理端 · 商品审核。
 */
@Tag(name = "11. 管理-商品审核", description = "待审核列表、通过/驳回、强制下架")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminService adminService;

    @Operation(summary = "商品列表（可按状态筛选，默认全部）")
    @PreAuthorize("hasAuthority('product:audit')")
    @GetMapping
    public Result<PageResult<ProductListResponse>> list(@RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.pageProducts(status, page, size));
    }

    @Operation(summary = "审核商品（通过上架 / 驳回需填原因）")
    @PreAuthorize("hasAuthority('product:audit')")
    @Audit(module = "商品", action = "审核商品")
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ProductAuditRequest request) {
        adminService.auditProduct(id, request);
        return Result.ok(request.getPass() ? "审核通过，商品已上架" : "已驳回该商品", null);
    }

    @Operation(summary = "强制下架")
    @PreAuthorize("hasAuthority('product:force-offline')")
    @Audit(module = "商品", action = "强制下架")
    @PostMapping("/{id}/force-offline")
    public Result<Void> forceOffline(@PathVariable Long id) {
        adminService.forceOffline(id);
        return Result.ok("已强制下架", null);
    }
}
