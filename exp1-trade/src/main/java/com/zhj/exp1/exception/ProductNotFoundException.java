package com.zhj.exp1.exception;

/**
 * 商品不存在异常
 */
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}