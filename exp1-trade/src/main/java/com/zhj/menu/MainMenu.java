package com.zhj.menu;

import com.zhj.model.Buyer;
import com.zhj.model.Seller;
import com.zhj.model.User;
import com.zhj.repository.DataStore;
import com.zhj.service.UserService;
import com.zhj.util.ConsoleUtil;
import com.zhj.util.InputUtil;

/**
 * 主菜单：登录 / 注册 / 退出
 */
public class MainMenu extends BaseMenu {

    private final UserService userService = new UserService();

    @Override
    public void show() {
        while (true) {
            ConsoleUtil.printTitle("校园二手商品交易管理系统");
            System.out.println("  当前用户数：" + DataStore.getInstance().getUserCount()
                    + "  商品数：" + DataStore.getInstance().getProductCount());
            ConsoleUtil.printLine();
            printOptions("登录", "注册", "退出系统");

            int choice = readChoice(1, 3);
            switch (choice) {
                case 1 -> doLogin();
                case 2 -> doRegister();
                case 3 -> {
                    ConsoleUtil.printInfo("感谢使用，再见！");
                    InputUtil.close();
                    System.exit(0);
                }
            }
        }
    }

    private void doLogin() {
        ConsoleUtil.printSubTitle("用户登录");
        String username = InputUtil.readString("用户名：");
        String password = InputUtil.readString("密  码：");

        safeExecute(() -> {
            User user = userService.login(username, password);
            ConsoleUtil.printSuccess("登录成功，欢迎你，" + user.getUsername() + "！");
            InputUtil.pause();
            dispatchByRole(user);
        });
    }

    private void doRegister() {
        ConsoleUtil.printSubTitle("用户注册");
        System.out.println("请选择注册类型：");
        printOptions("买家", "卖家", "返回");

        int type = readChoice(1, 3);
        if (type == 3) return;

        String username = InputUtil.readString("用户名：");
        String password = InputUtil.readString("密  码：");
        String phone = InputUtil.readString("手机号：");

        if (type == 1) {
            safeExecute(() -> {
                Buyer buyer = userService.registerBuyer(username, password, phone);
                ConsoleUtil.printSuccess("买家注册成功！赠送体验金 ￥1000.00，你的ID：" + buyer.getId());
                InputUtil.pause();
            });
        } else {
            String shopName = InputUtil.readString("店铺名称：");
            safeExecute(() -> {
                Seller seller = userService.registerSeller(username, password, phone, shopName);
                ConsoleUtil.printSuccess("卖家注册成功！你的ID：" + seller.getId());
                InputUtil.pause();
            });
        }
    }

    /**
     * 根据用户角色进入对应菜单
     */
    private void dispatchByRole(User user) {
        switch (user.getRole()) {
            case BUYER -> new BuyerMenu(user).show();
            case SELLER -> new SellerMenu(user).show();
            case ADMIN -> new AdminMenu(user).show();
        }
    }
}