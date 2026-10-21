package com.zhj.util;

import java.util.Scanner;

/**
 * 命令行输入工具类
 * 统一封装 Scanner，避免每个菜单里重复处理输入异常
 */
public class InputUtil {

    // 全局共享一个 Scanner，避免重复创建
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * 读取一行字符串，不允许为空
     *
     * @param prompt 提示信息
     * @return 用户输入的非空字符串
     */
    public static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("⚠ 输入不能为空，请重新输入");
                continue;
            }
            return input;
        }
    }

    /**
     * 读取一个整数，非法输入会提示重试
     *
     * @param prompt 提示信息
     * @return 用户输入的整数
     */
    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠ 请输入合法的整数");
            }
        }
    }

    /**
     * 读取一个整数，限定范围
     *
     * @param prompt 提示信息
     * @param min    最小值（含）
     * @param max    最大值（含）
     * @return 范围内的整数
     */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value < min || value > max) {
                System.out.printf("⚠ 请输入 %d ~ %d 之间的整数%n", min, max);
                continue;
            }
            return value;
        }
    }

    /**
     * 读取一个小数
     */
    public static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠ 请输入合法的小数");
            }
        }
    }

    /**
     * 等待用户按回车继续（用于菜单操作后暂停）
     */
    public static void pause() {
        System.out.print("\n按回车键继续...");
        scanner.nextLine();
    }

    /**
     * 关闭 Scanner（程序退出时调用）
     */
    public static void close() {
        scanner.close();
    }
}