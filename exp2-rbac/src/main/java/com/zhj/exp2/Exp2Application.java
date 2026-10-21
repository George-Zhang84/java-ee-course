package com.zhj.exp2;

import com.zhj.exp2.menu.MainMenu;
import com.zhj.exp2.model.User;
import com.zhj.exp2.service.AuthService;

import java.util.Scanner;

public class Exp2Application {
    public static void main(String[] args) {
        AuthService authService = new AuthService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================");
        System.out.println("  欢迎使用 RBAC 权限管理系统");
        System.out.println("=================================");

        while (true) {
            System.out.print("用户名：");
            String username = scanner.nextLine();
            if ("exit".equalsIgnoreCase(username)) {
                System.out.println("系统关闭。");
                System.exit(0);
            }

            System.out.print("密  码：");
            String password = scanner.nextLine();

            try {
                // 1. 登录
                User user = authService.login(username, password);
                System.out.println("✔ 登录成功，欢迎你 " + user.getUsername());

                // 2. 进入主菜单
                new MainMenu(user).show();
                System.out.println("已退出登录。\n");
                System.out.println("---------------------------------");

            } catch (Exception e) {
                System.out.println("✘ 登录失败：" + e.getMessage() + "\n");
            }
        }
    }
}