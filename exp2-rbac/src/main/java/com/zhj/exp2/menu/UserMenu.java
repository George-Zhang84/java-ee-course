package com.zhj.exp2.menu;

import com.zhj.exp2.annotation.RequiresPermission;
import com.zhj.exp2.audit.AuditLogger;
import com.zhj.exp2.infrastructure.exception.BusinessException;
import com.zhj.exp2.model.Role;
import com.zhj.exp2.model.User;
import com.zhj.exp2.service.RoleService;
import com.zhj.exp2.service.UserService;

import java.util.List;
import java.util.Scanner;

public class UserMenu extends BaseMenu {

    private final UserService userService = new UserService();
    private final RoleService roleService = new RoleService();
    private final Scanner scanner = new Scanner(System.in);

    public UserMenu(User currentUser) {
        super(currentUser);
    }

    @Override
    public void show() {
        while (true) {
            System.out.println("\n=== 用户管理 ===");
            System.out.println("1. 查看所有用户 (需要 user:view 权限)");
            System.out.println("2. 新增用户 (需要 user:create 权限)");
            System.out.println("3. 删除用户 (需要 user:delete 权限)");
            System.out.println("4. 启用/禁用用户 (需要 user:status 权限)");
            System.out.println("0. 返回上一级");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> executeMethod("listUsers");
                case "2" -> executeMethod("createUser");
                case "3" -> executeMethod("deleteUser");
                case "4" -> executeMethod("toggleUserStatus");
                case "0" -> {
                    return;
                }
                default -> System.out.println("无效输入");
            }
        }
    }

    @RequiresPermission("user:view")
    private void listUsers() {
        System.out.println("\n--- 用户列表 ---");
        List<User> users = userService.getAllUsers();
        System.out.printf("%-5s %-15s %-10s %-15s\n", "ID", "用户名", "状态", "所属角色");
        System.out.println("--------------------------------------------------");
        for (User u : users) {
            System.out.printf("%-5d %-15s %-10s %-15s\n",
                    u.getId(), u.getUsername(), u.getStatus() == 1 ? "正常" : "禁用",
                    u.getRoleName() == null ? "无" : u.getRoleName());
        }
    }

    @RequiresPermission("user:create")
    private void createUser() {
        System.out.print("请输入新用户名：");
        String uname = scanner.nextLine();
        System.out.print("请输入初始密码：");
        String pwd = scanner.nextLine();

        System.out.println("\n请选择该用户的角色：");
        System.out.println("---------------------------------");
        List<Role> roles = roleService.getAllRoles();

        // 根据当前登录用户的角色，动态过滤可选角色（防越权创建）
        boolean isSuper = "SUPER_ADMIN".equals(userService.getUserRoleCode(currentUser));
        for (Role r : roles) {
            if (!isSuper && ("SUPER_ADMIN".equals(r.getRoleCode()) || "USER_MANAGER".equals(r.getRoleCode()))) {
                continue; // 权限不够则隐藏高权限角色
            }
            System.out.printf("[%d] %s (%s)\n", r.getId(), r.getRoleName(), r.getRoleCode());
        }
        System.out.println("---------------------------------");
        System.out.print("请输入角色ID：");

        try {
            int roleId = Integer.parseInt(scanner.nextLine());
            if (userService.createUser(currentUser, uname, pwd, roleId)) {
                System.out.println("✔ 创建用户 [" + uname + "] 成功");
                AuditLogger.logBusiness(currentUser.getUsername(), "创建了用户 " + uname + " 并分配角色ID=" + roleId);
            }
        } catch (NumberFormatException e) {
            System.out.println("✘ 角色ID必须是数字");
        } catch (Exception e) {
            System.out.println("✘ " + e.getMessage());
        }
    }

    @RequiresPermission("user:delete")
    private void deleteUser() {
        System.out.print("请输入要删除的用户ID：");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            if (userService.deleteUser(currentUser, id)) {
                System.out.println("✔ 删除成功");
                AuditLogger.logBusiness(currentUser.getUsername(), "删除了用户ID=" + id);
            }
        } catch (NumberFormatException e) {
            System.out.println("✘ 请输入数字格式的ID");
        } catch (BusinessException e) {
            System.out.println("✘ " + e.getMessage());
        }
    }

    @RequiresPermission("user:status")
    private void toggleUserStatus() {
        System.out.print("请输入要操作的用户ID：");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            System.out.print("请输入新状态 (1-正常, 0-禁用)：");
            int status = Integer.parseInt(scanner.nextLine());

            if (status != 0 && status != 1) {
                System.out.println("✘ 状态只能是 0 或 1");
                return;
            }

            if (userService.updateUserStatus(currentUser, id, status)) {
                System.out.println("✔ 状态更新成功！");
                AuditLogger.logBusiness(currentUser.getUsername(),
                        "修改了用户ID=" + id + " 的状态为 " + (status == 1 ? "正常" : "禁用"));
            }
        } catch (NumberFormatException e) {
            System.out.println("✘ 请输入数字格式");
        } catch (BusinessException e) {
            System.out.println("✘ " + e.getMessage());
        }
    }
}