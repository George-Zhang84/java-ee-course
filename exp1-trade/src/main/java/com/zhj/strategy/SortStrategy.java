package com.zhj.strategy;

import com.zhj.model.Product;

import java.util.List;

/**
 * 商品排序策略接口
 */
public interface SortStrategy {
    List<Product> sort(List<Product> products);
}