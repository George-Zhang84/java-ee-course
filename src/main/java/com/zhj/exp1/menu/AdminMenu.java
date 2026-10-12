package com.zhj.exp1.menu;

import com.zhj.exp1.model.Product;
import com.zhj.exp1.model.Review;
import com.zhj.exp1.model.User;
import com.zhj.exp1.model.enums.ProductStatus;
import com.zhj.exp1.model.enums.UserRole;
import com.zhj.exp1.repository.DataStore;
import com.zhj.exp1.service.ProductService;
import com.zhj.exp1.service.ReviewService;
import com.zhj.exp1.service.UserService;
import com.zhj.exp1.util.ConsoleUtil;
import com.zhj.exp1.util.InputUtil;

import java.util.List;

/**
 * 管理员菜单
 */
public class AdminMenu extends BaseMenu {

    private final UserService userService = new UserService();
    private final ProductService productService = new ProductService();
    private final ReviewService reviewService = new ReviewService();
    private final DataStore store = DataStore.getInstance();

    public AdminMenu(User user) {
        super(user);
    }

    @Override
    public void show() {
        while (true) {
            ConsoleUtil.printTitle("管理员菜单 - " + currentUser.getUsername());
            printOptions("用户管理", "商品审核", "所有商品管理",
                    "所有订单查看", "评价管理", "数据统计", "返回主菜单");

            int choice = readChoice(1, 7);
            switch (choice) {
                case 1 -> userManagement();
                case 2 -> productAudit();
                case 3 -> allProducts();
                case 4 -> allOrders();
                case 5 -> reviewManagement();
                case 6 -> statistics();
                case 7 -> {
                    logout();
                    return;
                }
            }
        }
    }

    // ================== 用户管理 ==================
    private void userManagement() {
        while (true) {
            ConsoleUtil.printSubTitle("用户管理");
            printOptions("查看所有用户", "按角色筛选", "禁用用户", "启用用户", "重置密码", "返回");

            int choice = readChoice(1, 6);
            switch (choice) {
                case 1 -> listUsers(userService.listAllUsers());
                case 2 -> {
                    System.out.println("  [1] 买家  [2] 卖家  [3] 管理员");
                    int r = readChoice(1, 3);
                    UserRole role = switch (r) {
                        case 1 -> UserRole.BUYER;
                        case 2 -> UserRole.SELLER;
                        default -> UserRole.ADMIN;
                    };
                    listUsers(userService.listUsersByRole(role));
                }
                case 3 -> {
                    String uid = InputUtil.readString("请输入用户ID：");
                    safeExecute(() -> {
                        userService.disableUser(uid);
                        ConsoleUtil.printSuccess("用户已禁用");
                        InputUtil.pause();
                    });
                }
                case 4 -> {
                    String uid = InputUtil.readString("请输入用户ID：");
                    safeExecute(() -> {
                        userService.enableUser(uid);
                        ConsoleUtil.printSuccess("用户已启用");
                        InputUtil.pause();
                    });
                }
                case 5 -> {
                    String uid = InputUtil.readString("请输入用户ID：");
                    safeExecute(() -> {
                        userService.resetPassword(uid);
                        ConsoleUtil.printSuccess("密码已重置为 123456");
                        InputUtil.pause();
                    });
                }
                case 6 -> {
                    return;
                }
            }
        }
    }

    private void listUsers(List<User> users) {
        ConsoleUtil.printSubTitle("用户列表（共 " + users.size() + " 人）");
        System.out.printf("  %-6s %-15s %-8s %-10s%n", "ID", "用户名", "角色", "状态");
        ConsoleUtil.printLine();
        users.stream().limit(20).forEach(u ->
                System.out.printf("  %-6s %-15s %-8s %-10s%n",
                        u.getId(), u.getUsername(), u.getRole(), u.getStatus()));
        if (users.size() > 20) {
            System.out.println("  ...（仅显示前20条）");
        }
        InputUtil.pause();
    }

    // ================== 商品审核 ==================
    private void productAudit() {
        ConsoleUtil.printSubTitle("待审核商品");
        List<Product> pendings = productService.listPendingProducts();
        if (pendings.isEmpty()) {
            ConsoleUtil.printWarning("暂无待审核商品");
            InputUtil.pause();
            return;
        }
        for (Product p : pendings) {
            System.out.printf("  %-6s %-22s ￥%-8.2f 卖家:%s%n",
                    p.getId(), p.getName(), p.getPrice(), p.getSellerId());
        }
        ConsoleUtil.printLine();
        printOptions("审核通过", "驳回", "返回");
        int choice = readChoice(1, 3);

        switch (choice) {
            case 1 -> {
                String pid = InputUtil.readString("请输入商品ID：");
                safeExecute(() -> {
                    productService.approveProduct(pid);
                    ConsoleUtil.printSuccess("已通过审核");
                    InputUtil.pause();
                });
            }
            case 2 -> {
                String pid = InputUtil.readString("请输入商品ID：");
                safeExecute(() -> {
                    productService.rejectProduct(pid);
                    ConsoleUtil.printSuccess("已驳回");
                    InputUtil.pause();
                });
            }
            default -> {
            }
        }
    }

    // ================== 所有商品 ==================
    private void allProducts() {
        ConsoleUtil.printSubTitle("所有商品（共 " + store.getProductCount() + " 件）");
        System.out.printf("  %-6s %-22s ￥%-8s %-8s %-6s%n",
                "ID", "名称", "价格", "状态", "卖家");
        ConsoleUtil.printLine();
        store.getAllProducts().stream().limit(30).forEach(p ->
                System.out.printf("  %-6s %-22s ￥%-7.2f %-8s %-6s%n",
                        p.getId(), truncate(p.getName(), 20), p.getPrice(),
                        p.getStatus(), p.getSellerId()));
        if (store.getProductCount() > 30) {
            System.out.println("  ...（仅显示前30条）");
        }
        ConsoleUtil.printLine();

        String op = InputUtil.readString("是否下架某商品？(y/n)：").toLowerCase();
        if (op.equals("y")) {
            String pid = InputUtil.readString("商品ID：");
            safeExecute(() -> {
                productService.adminOffShelf(pid);
                ConsoleUtil.printSuccess("商品已强制下架");
                InputUtil.pause();
            });
        }
    }

    // ================== 所有订单 ==================
    private void allOrders() {
        ConsoleUtil.printSubTitle("所有订单（共 " + store.getOrderCount() + " 单）");
        System.out.printf("  %-6s %-6s %-6s %-6s %-10s%n",
                "订单ID", "买家", "卖家", "商品", "状态");
        ConsoleUtil.printLine();
        store.getAllOrders().stream().limit(30).forEach(o ->
                System.out.printf("  %-6s %-6s %-6s %-6s %-10s%n",
                        o.getId(), o.getBuyerId(), o.getSellerId(),
                        o.getProductId(), o.getStatus()));
        if (store.getOrderCount() > 30) {
            System.out.println("  ...（仅显示前30条）");
        }
        InputUtil.pause();
    }

    // ================== 评价管理 ==================
    private void reviewManagement() {
        ConsoleUtil.printSubTitle("所有评价（共 " + store.getReviewCount() + " 条）");
        List<Review> reviews = reviewService.listAll();
        if (reviews.isEmpty()) {
            ConsoleUtil.printWarning("暂无评价");
            InputUtil.pause();
            return;
        }
        reviews.stream().limit(20).forEach(r -> System.out.println("  " + r));
        if (reviews.size() > 20) System.out.println("  ...（仅显示前20条）");
        ConsoleUtil.printLine();

        String op = InputUtil.readString("是否删除某评价？(y/n)：").toLowerCase();
        if (op.equals("y")) {
            String rid = InputUtil.readString("评价ID：");
            safeExecute(() -> {
                reviewService.deleteReview(rid);
                ConsoleUtil.printSuccess("评价已删除");
                InputUtil.pause();
            });
        }
    }

    // ================== 数据统计 ==================
    private void statistics() {
        ConsoleUtil.printSubTitle("系统数据统计");
        System.out.println("  用户总数：  " + store.getUserCount());
        System.out.println("  - 买家数：  " + store.getAllUsers().stream()
                .filter(u -> u.getRole() == UserRole.BUYER).count());
        System.out.println("  - 卖家数：  " + store.getAllUsers().stream()
                .filter(u -> u.getRole() == UserRole.SELLER).count());
        System.out.println("  - 管理员：  " + store.getAllUsers().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN).count());
        ConsoleUtil.printLine();
        System.out.println("  商品总数：  " + store.getProductCount());
        for (ProductStatus s : ProductStatus.values()) {
            long cnt = store.findProductsByStatus(s).size();
            if (cnt > 0) System.out.println("  - " + s + "：" + cnt);
        }
        ConsoleUtil.printLine();
        System.out.println("  订单总数：  " + store.getOrderCount());
        System.out.println("  评价总数：  " + store.getReviewCount());
        InputUtil.pause();
    }

    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 2) + ".." : s;
    }
}