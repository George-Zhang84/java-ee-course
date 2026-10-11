package com.zhj.exp1.util;

/**
 * 命令行输出工具类
 * 统一管理分隔线、标题、表格等显示格式
 */
public class ConsoleUtil {

    private static final int WIDTH = 60;   // 分隔线宽度

    /**
     * 打印分隔线
     */
    public static void printLine() {
        System.out.println("-".repeat(WIDTH));
    }

    /**
     * 打印双线分隔线
     */
    public static void printDoubleLine() {
        System.out.println("=".repeat(WIDTH));
    }

    /**
     * 打印带边框的标题
     */
    public static void printTitle(String title) {
        printDoubleLine();
        // 居中显示
        int padding = (WIDTH - title.length()) / 2;
        System.out.println(" ".repeat(Math.max(0, padding)) + title);
        printDoubleLine();
    }

    /**
     * 打印子标题
     */
    public static void printSubTitle(String subtitle) {
        System.out.println("\n>>> " + subtitle);
        printLine();
    }

    /**
     * 打印成功提示（绿色在部分终端有效，普通控制台只是加符号）
     */
    public static void printSuccess(String message) {
        System.out.println("✔ " + message);
    }

    /**
     * 打印错误提示
     */
    public static void printError(String message) {
        System.out.println("✘ " + message);
    }

    /**
     * 打印警告提示
     */
    public static void printWarning(String message) {
        System.out.println("⚠ " + message);
    }

    /**
     * 打印普通信息
     */
    public static void printInfo(String message) {
        System.out.println("ℹ " + message);
    }
}