package com.zhj.exp4.controller;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.AuditLogQuery;
import com.zhj.exp4.dto.response.AuditLogResponse;
import com.zhj.exp4.dto.response.CategoryStat;
import com.zhj.exp4.dto.response.DashboardSummary;
import com.zhj.exp4.dto.response.HotProduct;
import com.zhj.exp4.dto.response.LoginLogResponse;
import com.zhj.exp4.dto.response.TrendPoint;
import com.zhj.exp4.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 · 数据看板与审计日志。
 *
 * <p>看板数据全部来自聚合 SQL（StatisticsMapper），不做全量加载后内存统计。
 */
@Tag(name = "13. 管理-看板与审计", description = "汇总指标、趋势、分类占比、热销榜、审计与登录日志")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminService adminService;

    @Operation(summary = "看板汇总指标")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/dashboard/summary")
    public Result<DashboardSummary> summary() {
        return Result.ok(adminService.summary());
    }

    @Operation(summary = "近 N 日趋势（默认 7 天）")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/dashboard/trend")
    public Result<List<TrendPoint>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(adminService.trend(days));
    }

    @Operation(summary = "分类商品占比")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/dashboard/categories")
    public Result<List<CategoryStat>> categories() {
        return Result.ok(adminService.categoryStats());
    }

    @Operation(summary = "热销商品 TopN")
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping("/dashboard/hot-products")
    public Result<List<HotProduct>> hotProducts(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(adminService.hotProducts(limit));
    }

    @Operation(summary = "操作审计日志")
    @PreAuthorize("hasAuthority('audit:view')")
    @GetMapping("/audit-logs")
    public Result<PageResult<AuditLogResponse>> auditLogs(AuditLogQuery query) {
        return Result.ok(adminService.pageAuditLogs(query));
    }

    @Operation(summary = "登录日志")
    @PreAuthorize("hasAuthority('audit:view')")
    @GetMapping("/login-logs")
    public Result<PageResult<LoginLogResponse>> loginLogs(@RequestParam(required = false) String username,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.pageLoginLogs(username, page, size));
    }
}
