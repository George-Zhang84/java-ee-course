package com.zhj.exp1.menu;

import com.zhj.exp1.exception.BusinessException;
import com.zhj.exp1.model.User;
import com.zhj.exp1.util.ConsoleUtil;
import com.zhj.exp1.util.InputUtil;

/**
 * 菜单抽象基类（模板方法模式）
 * 定义了菜单的通用行为，子类只需实现 show() 方法
 */
public abstract class BaseMenu {

    /**
     * 当前登录用户
     */
    protected User currentUser;

    public BaseMenu() {
    }

    public BaseMenu(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * 子类必须实现的菜单展示方法
     */
    public abstract void show();

    /**
     * 统一的异常处理包装
     * 子类的业务调用都通过这个方法来捕获异常，避免程序崩溃
     */
    protected void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (BusinessException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (Exception e) {
            ConsoleUtil.printError("系统异常：" + e.getMessage());
        }
    }

    /**
     * 打印菜单选项
     */
    protected void printOptions(String... options) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  [%d] %s%n", i + 1, options[i]);
        }
        ConsoleUtil.printLine();
    }

    /**
     * 读取用户选择（限定范围）
     */
    protected int readChoice(int min, int max) {
        return InputUtil.readInt("请选择操作：", min, max);
    }

    /**
     * 退出登录：返回主菜单
     */
    protected void logout() {
        ConsoleUtil.printInfo("已退出登录，返回主菜单");
        new MainMenu().show();
    }
}