package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.ProductCreateRequest;
import com.zhj.exp4.dto.request.ProductQuery;
import com.zhj.exp4.dto.request.ProductStatusRequest;
import com.zhj.exp4.dto.request.ProductUpdateRequest;
import com.zhj.exp4.dto.response.ProductDetailResponse;
import com.zhj.exp4.dto.response.ProductListResponse;
import com.zhj.exp4.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口。
 *
 * <p>两类权限控制同时存在：
 * <ul>
 *   <li>{@code @PreAuthorize} 判断"这个账号有没有发布/编辑功能"（纵向权限）；</li>
 *   <li>Service 内部判断"这个商品是不是你的"（横向权限）。</li>
 * </ul>
 * 只做前者的话，任何卖家都能改别人商品；只做后者则买家也能调用发布接口。
 */
@Tag(name = "03. 商品", description = "浏览、搜索、发布、编辑、上下架、收藏")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品列表（搜索/分类/价格区间/排序，游客可访问）")
    @GetMapping
    public Result<PageResult<ProductListResponse>> list(ProductQuery query) {
        return Result.ok(productService.pagePublic(query));
    }

    @Operation(summary = "我发布的商品")
    @GetMapping("/mine")
    public Result<PageResult<ProductListResponse>> mine(ProductQuery query) {
        return Result.ok(productService.pageMine(query));
    }

    @Operation(summary = "商品详情（浏览量 +1）")
    @GetMapping("/{id}")
    public Result<ProductDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @Operation(summary = "发布商品（进入待审核）")
    @PreAuthorize("hasAuthority('product:publish')")
    @Audit(module = "商品", action = "发布商品")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCreateRequest request) {
        return Result.ok("发布成功，等待管理员审核", productService.create(request));
    }

    @Operation(summary = "编辑商品")
    @PreAuthorize("hasAuthority('product:edit')")
    @Audit(module = "商品", action = "编辑商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        productService.update(id, request);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "上架 / 下架")
    @PreAuthorize("hasAuthority('product:offline')")
    @Audit(module = "商品", action = "上下架")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        productService.changeStatus(id, request.getStatus());
        return Result.ok("操作成功", null);
    }

    @Operation(summary = "删除商品（逻辑删除）")
    @PreAuthorize("hasAuthority('product:edit')")
    @Audit(module = "商品", action = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok("删除成功", null);
    }
}
