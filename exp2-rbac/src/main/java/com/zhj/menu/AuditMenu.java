package com.zhj.menu;

import com.zhj.annotation.RequiresPermission;
import com.zhj.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class AuditMenu extends BaseMenu {

    private final Scanner scanner = new Scanner(System.in);

    public AuditMenu(User currentUser) {
        super(currentUser);
    }

    @Override
    public void show() {
        while (true) {
            System.out.println("\n=== 权限审计 ===");
            System.out.println("1. 查看最新审计日志 (需要 menu:audit 权限)");
            System.out.println("0. 返回上一级");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                executeMethod("viewLogs");
            } else if ("0".equals(choice)) {
                return;
            }
        }
    }

    @RequiresPermission("menu:audit")
    private void viewLogs() {
        System.out.println("\n--- 最近 20 条审计日志 ---");
        try {
            List<String> lines = Files.readAllLines(Paths.get("logs/audit.log"));
            int start = Math.max(0, lines.size() - 20);
            for (int i = start; i < lines.size(); i++) {
                System.out.println(lines.get(i));
            }
        } catch (IOException e) {
            System.out.println("✘ 读取日志失败，可能还没有日志文件。");
        }
    }
}