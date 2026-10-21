package com.zhj.util;

/**
 * 命令行输出工具类
 * 统一管理分隔线、标题、表格等显示格式
 */
public class ConsoleUtil {

    /**
     * 分隔线宽度
     */
    public static final int WIDTH = 90;

    /**
     * 打印分隔线
     */
    public static void printLine() {
        System.out.println("-".repeat(WIDTH));
    }

    /** 打印双线分隔线 */
    public static void printDoubleLine() {
        System.out.println("=".repeat(WIDTH));
    }

    /** 打印带边框的标题 */
    public static void printTitle(String title) {
        printDoubleLine();
        int padding = (WIDTH - title.length() * 2) / 2;   // 中文按2算宽度
        System.out.println(" ".repeat(Math.max(0, padding)) + title);
        printDoubleLine();
    }

    /** 打印子标题 */
    public static void printSubTitle(String subtitle) {
        System.out.println("\n>>> " + subtitle);
        printLine();
    }

    public static void printSuccess(String message) {
        System.out.println("✔ " + message);
    }

    public static void printError(String message) {
        System.out.println("✘ " + message);
    }

    public static void printWarning(String message) {
        System.out.println("⚠ " + message);
    }

    public static void printInfo(String message) {
        System.out.println("ℹ " + message);
    }

    // ================== 中文对齐核心工具 ==================

    /**
     * 计算字符串显示宽度（中文/全角占2，英文/半角占1）
     */
    public static int displayWidth(String s) {
        if (s == null) return 0;
        int w = 0;
        for (char c : s.toCharArray()) {
            // 常用CJK汉字、全角标点判断
            w += (c > 0x7F) ? 2 : 1;
        }
        return w;
    }

    /**
     * 按显示宽度左对齐补空格
     * @param s 待对齐的字符串
     * @param width 目标显示宽度
     */
    public static String padRight(String s, int width) {
        if (s == null) s = "";
        int pad = width - displayWidth(s);
        return pad > 0 ? s + " ".repeat(pad) : s;
    }

    /**
     * 按显示宽度右对齐补空格
     */
    public static String padLeft(String s, int width) {
        if (s == null) s = "";
        int pad = width - displayWidth(s);
        return pad > 0 ? " ".repeat(pad) + s : s;
    }

    /**
     * 打印一行表格数据（自动按显示宽度对齐）
     * @param widths 每列宽度（显示宽度）
     * @param cells 每列内容
     */
    public static void printRow(int[] widths, String... cells) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < cells.length && i < widths.length; i++) {
            sb.append(padRight(cells[i], widths[i]));
            sb.append(" ");
        }
        System.out.println(sb);
    }
}