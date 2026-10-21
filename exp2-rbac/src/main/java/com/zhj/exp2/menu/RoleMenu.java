package com.zhj.exp2.menu;

import com.zhj.exp2.annotation.RequiresPermission;
import com.zhj.exp2.model.Role;
import com.zhj.exp2.model.User;
import com.zhj.exp2.service.RoleService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RoleMenu extends BaseMenu {
    private final RoleService roleService = new RoleService();
    private final Scanner scanner = new Scanner(System.in);

    public RoleMenu(User currentUser) {
        super(currentUser);
    }

    @Override
    public void show() {
        while (true) {
            System.out.println("\n=== 角色管理 ===");
            System.out.println("1. 查看所有角色 (需要 menu:role 权限)");
            System.out.println("2. 为角色分配权限 (需要 role:assign 权限)");
            System.out.println("0. 返回上一级");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> executeMethod("listRoles");
                case "2" -> executeMethod("assignPerm");
                case "0" -> {
                    return;
                }
                default -> System.out.println("无效输入");
            }
        }
    }

    @RequiresPermission("menu:role")
    private void listRoles() {
        System.out.println("\n--- 角色列表 ---");
        List<Role> roles = roleService.getAllRoles();
        System.out.printf("%-5s %-15s %-15s\n", "ID", "角色编码", "角色名称");
        for (Role r : roles) {
            System.out.printf("%-5d %-15s %-15s\n", r.getId(), r.getRoleCode(), r.getRoleName());
        }
    }

    @RequiresPermission("role:assign")
    private void assignPerm() {
        System.out.print("请输入要操作的角色ID：");
        try {
            int roleId = Integer.parseInt(scanner.nextLine());

            // 前置校验：角色是否存在
            List<Role> roles = roleService.getAllRoles();
            boolean roleExists = roles.stream().anyMatch(r -> r.getId() == roleId);
            if (!roleExists) {
                System.out.println("✘ 操作失败：角色ID " + roleId + " 不存在！");
                return;
            }

            System.out.println("⚠ 注意：这是覆盖操作，将替换该角色原有的所有权限。");
            System.out.println("请输入要分配的权限ID列表（用逗号分隔，如：1,2,3）：");
            String[] idsStr = scanner.nextLine().split(",");
            List<Integer> permIds = new ArrayList<>();
            for (String s : idsStr) {
                permIds.add(Integer.parseInt(s.trim()));
            }

            roleService.assignPermissions(roleId, permIds);
            System.out.println("✔ 权限分配成功！");

        } catch (NumberFormatException e) {
            System.out.println("✘ 输入格式错误，请输入数字ID");
        } catch (Exception e) {
            System.out.println("✘ " + e.getMessage()); // 捕获 DAO 抛上来的业务异常
        }
    }
}