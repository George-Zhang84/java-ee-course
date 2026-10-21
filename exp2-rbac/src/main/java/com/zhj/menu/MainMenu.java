package com.zhj.menu;

import com.zhj.model.User;

import java.util.Scanner;

public class MainMenu extends BaseMenu {
    private final Scanner scanner = new Scanner(System.in);

    public MainMenu(User currentUser) {
        super(currentUser);
    }

    @Override
    public void show() {
        while (true) {
            System.out.println("\n=== 系统主菜单 ===");
            System.out.println("欢迎，" + currentUser.getUsername());

            // 根据当前用户的权限动态显示菜单
            boolean hasUserMenu = authService.hasPermission(currentUser, "menu:user");
            boolean hasRoleMenu = authService.hasPermission(currentUser, "menu:role");
            boolean hasPermMenu = authService.hasPermission(currentUser, "menu:perm");
            boolean hasAuditMenu = authService.hasPermission(currentUser, "menu:audit");

            if (hasUserMenu) System.out.println("1. 用户管理");
            if (hasRoleMenu) System.out.println("2. 角色管理");
            if (hasPermMenu) System.out.println("3. 权限总览");
            if (hasAuditMenu) System.out.println("4. 权限审计");
            System.out.println("0. 退出登录");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> {
                    if (hasUserMenu) new UserMenu(currentUser).show();
                    else System.out.println("✘ 无权限");
                }
                case "2" -> {
                    if (hasRoleMenu) new RoleMenu(currentUser).show();
                    else System.out.println("✘ 无权限");
                }
                case "3" -> {
                    if (hasPermMenu) new PermMenu(currentUser).show();
                    else System.out.println("✘ 无权限");
                }
                case "4" -> {
                    if (hasAuditMenu) new AuditMenu(currentUser).show();
                    else System.out.println("✘ 无权限");
                }
                case "0" -> {
                    return;
                }
                default -> System.out.println("无效输入");
            }
        }
    }
}