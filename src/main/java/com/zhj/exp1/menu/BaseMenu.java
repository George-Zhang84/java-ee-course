package com.zhj.exp1.menu;

import com.zhj.exp1.exception.BusinessException;
import com.zhj.exp1.model.User;
import com.zhj.exp1.util.ConsoleUtil;
import com.zhj.exp1.util.InputUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
     * 通用分页浏览（纯展示）
     */
    protected <T> void browsePaged(List<T> list, int pageSize, String title,
                                   Consumer<List<T>> renderPage) {
        browsePaged(list, pageSize, title, renderPage, null, null);
    }

    /**
     * 通用分页浏览（支持额外操作）
     *
     * @param list         完整数据列表
     * @param pageSize     每页条数
     * @param title        标题
     * @param renderPage   渲染一页数据的回调
     * @param extraHint    额外操作提示（如 "[v]查看详情"），null 表示无
     * @param extraActions 额外操作映射：key=用户输入，value=要执行的动作
     */
    protected <T> void browsePaged(List<T> list, int pageSize, String title,
                                   Consumer<List<T>> renderPage,
                                   String extraHint,
                                   Map<String, Runnable> extraActions) {
        if (list == null || list.isEmpty()) {
            ConsoleUtil.printWarning("暂无数据");
            InputUtil.pause();
            return;
        }

        int page = 1;
        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / pageSize));

        while (true) {
            int from = (page - 1) * pageSize;
            int to = Math.min(from + pageSize, list.size());
            List<T> pageData = list.subList(from, to);

            ConsoleUtil.printSubTitle(title + " - 第 " + page + "/" + totalPages
                    + " 页（共 " + list.size() + " 条）");
            renderPage.accept(pageData);
            ConsoleUtil.printLine();

            // 操作提示
            StringBuilder sb = new StringBuilder();
            if (page > 1) sb.append("[p]上一页 ");
            if (page < totalPages) sb.append("[n]下一页 ");
            if (extraHint != null && !extraHint.isEmpty()) sb.append(extraHint).append(" ");
            sb.append("[q]返回");
            System.out.println("  " + sb);

            String op = InputUtil.readString("请输入操作：").toLowerCase();
            switch (op) {
                case "n" -> {
                    if (page < totalPages) page++;
                }
                case "p" -> {
                    if (page > 1) page--;
                }
                case "q" -> {
                    return;
                }
                default -> {
                    if (extraActions != null && extraActions.containsKey(op)) {
                        extraActions.get(op).run();
                    } else {
                        ConsoleUtil.printWarning("无效操作");
                    }
                }
            }
        }
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