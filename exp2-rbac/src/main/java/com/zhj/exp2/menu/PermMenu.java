package com.zhj.exp2.menu;

import com.zhj.exp2.annotation.RequiresPermission;
import com.zhj.exp2.model.Permission;
import com.zhj.exp2.model.User;
import com.zhj.exp2.service.PermService;

import java.util.List;
import java.util.Scanner;

public class PermMenu extends BaseMenu {

    private final PermService permService = new PermService();
    private final Scanner scanner = new Scanner(System.in);

    public PermMenu(User currentUser) {
        super(currentUser);
    }

    @Override
    public void show() {
        while (true) {
            System.out.println("\n=== 权限总览 ===");
            System.out.println("1. 查看所有权限列表 (需要 menu:perm 权限)");
            System.out.println("0. 返回上一级");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                executeMethod("listPermissions");
            } else if ("0".equals(choice)) {
                return;
            }
        }
    }

    @RequiresPermission("menu:perm")
    private void listPermissions() {
        System.out.println("\n--- 系统权限列表 ---");
        List<Permission> perms = permService.getAllPermissions();
        System.out.printf("%-5s %-15s %-15s %-10s\n", "ID", "权限码", "权限名称", "类型");
        System.out.println("------------------------------------------------------");
        for (Permission p : perms) {
            System.out.printf("%-5d %-15s %-15s %-10s\n",
                    p.getId(), p.getPermCode(), p.getPermName(), p.getPermType());
        }
    }
}