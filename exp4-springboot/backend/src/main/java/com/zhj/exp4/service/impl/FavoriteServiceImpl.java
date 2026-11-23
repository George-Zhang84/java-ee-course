package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.dto.request.ProductQuery;
import com.zhj.exp4.dto.response.ProductListResponse;
import com.zhj.exp4.entity.Favorite;
import com.zhj.exp4.entity.Product;
import com.zhj.exp4.enums.ProductStatus;
import com.zhj.exp4.mapper.FavoriteMapper;
import com.zhj.exp4.mapper.ProductMapper;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏夹实现。
 *
 * <p>并发下的"重复收藏"由数据库唯一索引兜底：
 * 先查再插存在竞态，因此捕获 {@link DuplicateKeyException} 并按成功处理（幂等语义）。
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    @Override
    public void add(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        Long userId = SecurityUtils.currentUserId();
        try {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 已收藏：幂等返回
        }
    }

    @Override
    public void remove(Long productId) {
        Long userId = SecurityUtils.currentUserId();
        favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
    }

    @Override
    public PageResult<ProductListResponse> pageMine(ProductQuery query) {
        Long userId = SecurityUtils.currentUserId();
        Page<Favorite> favoritePage = favoriteMapper.selectPage(
                Page.of(query.safePage(), query.safeSize()),
                Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));

        List<Long> productIds = favoritePage.getRecords().stream()
                .map(Favorite::getProductId)
                .toList();
        if (productIds.isEmpty()) {
            return PageResult.empty(favoritePage.getCurrent(), favoritePage.getSize());
        }
        // 注意保持收藏顺序：按 productIds 的顺序回填，而不是按商品查询结果顺序
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        List<ProductListResponse> records = productIds.stream()
                .map(productMap::get)
                .filter(p -> p != null)
                .map(this::toResponse)
                .toList();
        return new PageResult<>(records, favoritePage.getTotal(),
                favoritePage.getCurrent(), favoritePage.getSize());
    }

    private ProductListResponse toResponse(Product p) {
        return ProductListResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .price(p.getPrice())
                .coverUrl(p.getCoverUrl())
                .conditionLevel(p.getConditionLevel())
                .location(p.getLocation())
                .categoryId(p.getCategoryId())
                .sellerId(p.getSellerId())
                .allowBargain(p.getAllowBargain())
                .status(p.getStatus())
                .statusText(statusText(p.getStatus()))
                .views(p.getViews())
                .createTime(p.getCreateTime())
                .build();
    }

    private String statusText(String status) {
        try {
            return ProductStatus.valueOf(status).getDescription();
        } catch (Exception e) {
            return status;
        }
    }
}
