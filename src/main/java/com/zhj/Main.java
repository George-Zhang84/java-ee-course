package com.zhj;

import com.zhj.exp1.repository.DataInitializer;
import com.zhj.exp1.repository.DataStore;

public class Main {
    public static void main(String[] args) {
        // 初始化测试数据
        DataInitializer.init();

        // 验证数据是否正确生成
        DataStore store = DataStore.getInstance();
        System.out.println("用户总数：" + store.getUserCount());
        System.out.println("商品总数：" + store.getProductCount());

        // 打印前5个商品看看
        System.out.println("\n===== 前5个商品 =====");
        store.getAllProducts().stream()
                .limit(5)
                .forEach(System.out::println);

        // 打印前3个用户
        System.out.println("\n===== 前3个用户 =====");
        store.getAllUsers().stream()
                .limit(3)
                .forEach(System.out::println);
    }
}