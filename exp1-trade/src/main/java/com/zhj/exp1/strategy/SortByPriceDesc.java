package com.zhj.exp1.strategy;

import com.zhj.exp1.model.Product;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortByPriceDesc implements SortStrategy {
    @Override
    public List<Product> sort(List<Product> products) {
        return products.stream()
                .sorted(Comparator.comparingDouble(Product::getPrice).reversed())
                .collect(Collectors.toList());
    }
}