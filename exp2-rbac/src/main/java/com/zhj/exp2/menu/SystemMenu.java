package com.zhj.exp2.menu;

import com.zhj.exp2.annotation.RequiresPermission;
import com.zhj.exp2.model.User;

import java.util.Scanner;

public class SystemMenu extends BaseMenu {

    public SystemMenu(User currentUser) {
        super(currentUser);
    }

    @Override
    public void show() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== 系统菜单 ===");
            System.out.println("1. 执行受保护操作 (需要 user:delete 权限)");
            System.out.println("2. 执行无保护操作");
            System.out.println("0. 退出");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            if ("0".equals(choice)) break;
            if ("1".equals(choice)) {
                executeMethod("doProtectedAction");
            } else if ("2".equals(choice)) {
                executeMethod("doPublicAction");
            }
        }
    }

    @RequiresPermission("user:delete")
    public void doProtectedAction() {
        System.out.println("✔ 成功执行：删除用户操作！");
    }

    public void doPublicAction() {
        System.out.println("✔ 成功执行：公开操作！");
    }
}