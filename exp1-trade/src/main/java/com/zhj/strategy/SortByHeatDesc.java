package com.zhj.strategy;

import com.zhj.model.Product;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortByHeatDesc implements SortStrategy {
    @Override
    public List<Product> sort(List<Product> products) {
        return products.stream()
                .sorted(Comparator.comparingInt(Product::getViewCount).reversed())
                .collect(Collectors.toList());
    }
}