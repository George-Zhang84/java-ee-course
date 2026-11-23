package com.zhj.exp4.controller;

import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.response.CategoryResponse;
import com.zhj.exp4.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口（游客可访问，用于首页筛选与发布表单下拉）。
 */
@Tag(name = "02. 分类", description = "商品分类字典")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "启用中的分类列表")
    @GetMapping
    public Result<List<CategoryResponse>> list() {
        return Result.ok(categoryService.listEnabled());
    }
}
