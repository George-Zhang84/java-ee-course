package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.dto.response.CategoryResponse;
import com.zhj.exp4.entity.Category;
import com.zhj.exp4.mapper.CategoryMapper;
import com.zhj.exp4.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务实现。
 *
 * <p>分类属于低频变更的字典数据，后续可加缓存；当前数据量小，直接查库更简单，
 * 且保证管理员停用分类后立即生效。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> listEnabled() {
        List<Category> categories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
        return categories.stream().map(CategoryResponse::of).toList();
    }

    @Override
    public String requireEnabled(Long categoryId) {
        if (categoryId == null) {
            throw BizException.of("请选择商品分类");
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BizException.of("分类不存在");
        }
        if (category.getStatus() == null || category.getStatus() != 1) {
            throw BizException.of("该分类已停用，请选择其他分类");
        }
        return category.getName();
    }
}
