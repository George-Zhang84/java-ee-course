package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.ReviewCreateRequest;
import com.zhj.exp4.dto.response.ReviewResponse;
import com.zhj.exp4.service.ReviewService;
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
 * 评价接口。
 *
 * <p>商品评价查询挂在 {@code /api/products/{id}/reviews} 下（语义上属于商品资源），
 * 该路径在 SecurityConfig 中属于公开的 GET 商品接口，游客也能看评价。
 */
@Tag(name = "09. 评价", description = "商品评价、我的评价")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "发表评价（订单完成后，买卖双方各一次）")
    @PreAuthorize("hasAuthority('review:create')")
    @Audit(module = "评价", action = "发表评价")
    @PostMapping("/reviews")
    public Result<Void> create(@Valid @RequestBody ReviewCreateRequest request) {
        reviewService.create(request);
        return Result.ok("评价成功，感谢反馈", null);
    }

    @Operation(summary = "商品评价列表（游客可访问）")
    @GetMapping("/products/{productId}/reviews")
    public Result<PageResult<ReviewResponse>> byProduct(@PathVariable Long productId,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reviewService.pageByProduct(productId, page, size));
    }

    @Operation(summary = "我发出的评价")
    @GetMapping("/reviews/mine")
    public Result<PageResult<ReviewResponse>> mine(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reviewService.pageMine(page, size));
    }

    @Operation(summary = "我收到的评价（买家评我 / 卖家评我）")
    @GetMapping("/reviews/received")
    public Result<PageResult<ReviewResponse>> received(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reviewService.pageReceived(page, size));
    }
}
