package com.zhj.exp1.strategy;

import com.zhj.exp1.model.Product;

import java.util.List;

/**
 * 商品排序策略接口
 */
public interface SortStrategy {
    List<Product> sort(List<Product> products);
}