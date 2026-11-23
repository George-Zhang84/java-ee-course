package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.ProductQuery;
import com.zhj.exp4.dto.response.ProductListResponse;
import com.zhj.exp4.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收藏夹接口。收藏/取消收藏都是幂等操作，前端连点两次不会产生脏数据。
 */
@Tag(name = "04. 收藏", description = "收藏夹")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "我的收藏")
    @GetMapping
    public Result<PageResult<ProductListResponse>> list(ProductQuery query) {
        return Result.ok(favoriteService.pageMine(query));
    }

    @Operation(summary = "收藏商品")
    @Audit(module = "收藏", action = "收藏商品", saveParams = false)
    @PostMapping("/{productId}")
    public Result<Void> add(@PathVariable Long productId) {
        favoriteService.add(productId);
        return Result.ok("收藏成功", null);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        favoriteService.remove(productId);
        return Result.ok("已取消收藏", null);
    }
}
