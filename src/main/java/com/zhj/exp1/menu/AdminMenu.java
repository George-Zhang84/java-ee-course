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

    /**
     * 用户列表 - 分页显示
     */
    private void listUsers(List<User> users) {
        int[] widths = {8, 18, 12, 12};
        browsePaged(users, 20, "用户列表", pageData -> {
            ConsoleUtil.printRow(widths, "ID", "用户名", "角色", "状态");
            ConsoleUtil.printLine();
            for (User u : pageData) {
                ConsoleUtil.printRow(widths,
                        u.getId(), u.getUsername(),
                        u.getRole().toString(), u.getStatus().toString());
            }
        });
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
        int[] widths = {8, 24, 12, 16};
        ConsoleUtil.printRow(widths, "ID", "名称", "价格", "卖家");
        ConsoleUtil.printLine();
        for (Product p : pendings) {
            User seller = userService.getUserById(p.getSellerId());
            String sellerName = (seller != null) ? seller.getUsername() : p.getSellerId();
            ConsoleUtil.printRow(widths,
                    p.getId(),
                    truncate(p.getName(), 20),
                    String.format("￥%.2f", p.getPrice()),
                    sellerName);
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
        int[] widths = {8, 24, 12, 12, 14};
        List<Product> products = store.getAllProducts();

        // 分页浏览
        browsePaged(products, 15, "所有商品", pageData -> {
            ConsoleUtil.printRow(widths, "ID", "名称", "价格", "状态", "卖家");
            ConsoleUtil.printLine();
            for (Product p : pageData) {
                User seller = store.findUserById(p.getSellerId());
                String sellerName = (seller != null) ? seller.getUsername() : p.getSellerId();
                ConsoleUtil.printRow(widths,
                        p.getId(),
                        truncate(p.getName(), 20),
                        String.format("￥%.2f", p.getPrice()),
                        p.getStatus().toString(),
                        sellerName);
            }
        });

        // 翻页结束后，询问是否下架
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
        int[] widths = {10, 24, 14, 14, 10};
        List<com.zhj.exp1.model.Order> orders = store.getAllOrders();

        browsePaged(orders, 15, "所有订单", pageData -> {
            ConsoleUtil.printRow(widths, "订单ID", "商品名称", "买家", "卖家", "状态");
            ConsoleUtil.printLine();
            for (com.zhj.exp1.model.Order o : pageData) {
                Product p = store.findProductById(o.getProductId());
                User buyer = store.findUserById(o.getBuyerId());
                User seller = store.findUserById(o.getSellerId());
                String pName = (p != null) ? truncate(p.getName(), 20) : o.getProductId();
                String buyerName = (buyer != null) ? buyer.getUsername() : o.getBuyerId();
                String sellerName = (seller != null) ? seller.getUsername() : o.getSellerId();
                ConsoleUtil.printRow(widths,
                        o.getId(), pName, buyerName, sellerName,
                        o.getStatus().toString());
            }
        });
    }

    // ================== 评价管理 ==================
    private void reviewManagement() {
        List<Review> reviews = reviewService.listAll();
        int[] widths = {12, 18, 8, 30, 14};

        browsePaged(reviews, 15, "所有评价", pageData -> {
            ConsoleUtil.printRow(widths, "评价ID", "商品ID", "评分", "内容", "评价时间");
            ConsoleUtil.printLine();
            for (Review r : pageData) {
                ConsoleUtil.printRow(widths,
                        r.getId(), r.getOrderId(),
                        r.getRating() + "星",
                        truncate(r.getContent(), 28),
                        r.getCreateTime().toLocalDate().toString());
            }
        });

        // 翻页结束后，询问是否删除
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