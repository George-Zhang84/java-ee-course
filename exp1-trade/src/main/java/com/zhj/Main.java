package com.zhj;

import com.zhj.menu.MainMenu;
import com.zhj.repository.DataInitializer;

public class Main {
    public static void main(String[] args) {
        // 初始化测试数据（若已存在则不会重复生成，暂不判断）
        DataInitializer.init();
        // 启动主菜单
        new MainMenu().show();
    }
}