package com.zhj.exp1.menu;

import com.zhj.exp1.model.Order;
import com.zhj.exp1.model.Product;
import com.zhj.exp1.model.Review;
import com.zhj.exp1.model.Seller;
import com.zhj.exp1.model.User;
import com.zhj.exp1.model.enums.Category;
import com.zhj.exp1.model.enums.OrderStatus;
import com.zhj.exp1.service.OrderService;
import com.zhj.exp1.service.ProductService;
import com.zhj.exp1.service.ReviewService;
import com.zhj.exp1.util.ConsoleUtil;
import com.zhj.exp1.util.InputUtil;

import java.util.List;

/**
 * 卖家菜单
 */
public class SellerMenu extends BaseMenu {

    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final ReviewService reviewService = new ReviewService();

    public SellerMenu(User user) {
        super(user);
    }

    private Seller getSeller() {
        return (Seller) currentUser;
    }

    @Override
    public void show() {
        while (true) {
            Seller s = getSeller();
            ConsoleUtil.printTitle("卖家菜单 - " + s.getUsername()
                    + "  店铺：" + s.getShopName()
                    + "  评分：" + s.getRating()
                    + "  销量：" + s.getSoldCount());
            printOptions("发布商品", "我的商品", "订单管理", "我的评价", "返回主菜单");

            int choice = readChoice(1, 5);
            switch (choice) {
                case 1 -> publishProduct();
                case 2 -> myProducts();
                case 3 -> orderManagement();
                case 4 -> myReviews();
                case 5 -> {
                    logout();
                    return;
                }
            }
        }
    }

    private void publishProduct() {
        ConsoleUtil.printSubTitle("发布商品");
        String name = InputUtil.readString("商品名称：");
        String desc = InputUtil.readString("商品描述：");
        double price = InputUtil.readDouble("商品价格：");

        // 选择分类
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("  [%d] %s%n", i + 1, categories[i]);
        }
        int c = InputUtil.readInt("请选择分类：", 1, categories.length);

        safeExecute(() -> {
            Product p = productService.publishProduct(
                    currentUser.getId(), name, desc, price, categories[c - 1]);
            ConsoleUtil.printSuccess("商品发布成功，ID：" + p.getId() + "，等待管理员审核");
            InputUtil.pause();
        });
    }

    private void myProducts() {
        ConsoleUtil.printSubTitle("我的商品");
        List<Product> products = productService.listProductsBySeller(currentUser.getId());
        if (products.isEmpty()) {
            ConsoleUtil.printWarning("你还没有发布任何商品");
            InputUtil.pause();
            return;
        }

        System.out.printf("  %-6s %-22s %-10s %-8s%n", "ID", "名称", "价格", "状态");
        ConsoleUtil.printLine();
        for (Product p : products) {
            System.out.printf("  %-6s %-22s ￥%-9.2f %-8s%n",
                    p.getId(), truncate(p.getName(), 20), p.getPrice(), p.getStatus());
        }
        ConsoleUtil.printLine();

        printOptions("编辑商品", "下架商品", "重新上架", "返回");
        int choice = readChoice(1, 4);
        switch (choice) {
            case 1 -> editProduct();
            case 2 -> offShelf();
            case 3 -> reOnShelf();
            case 4 -> {
            }
        }
    }

    private void editProduct() {
        String pid = InputUtil.readString("请输入商品ID：");
        String name = InputUtil.readString("新名称：");
        String desc = InputUtil.readString("新描述：");
        double price = InputUtil.readDouble("新价格：");
        safeExecute(() -> {
            productService.editProduct(pid, currentUser.getId(), name, desc, price);
            ConsoleUtil.printSuccess("修改成功，商品重新进入待审核状态");
            InputUtil.pause();
        });
    }

    private void offShelf() {
        String pid = InputUtil.readString("请输入商品ID：");
        safeExecute(() -> {
            productService.offShelf(pid, currentUser.getId());
            ConsoleUtil.printSuccess("商品已下架");
            InputUtil.pause();
        });
    }

    private void reOnShelf() {
        String pid = InputUtil.readString("请输入商品ID：");
        safeExecute(() -> {
            productService.reOnShelf(pid, currentUser.getId());
            ConsoleUtil.printSuccess("商品已重新上架");
            InputUtil.pause();
        });
    }

    private void orderManagement() {
        while (true) {
            ConsoleUtil.printSubTitle("我收到的订单");
            List<Order> orders = orderService.listOrdersBySeller(currentUser.getId());
            if (orders.isEmpty()) {
                ConsoleUtil.printWarning("暂无订单");
                InputUtil.pause();
                return;
            }
            System.out.printf("  %-6s %-6s %-6s %-10s %-20s%n",
                    "订单ID", "商品ID", "买家ID", "状态", "下单时间");
            ConsoleUtil.printLine();
            for (Order o : orders) {
                System.out.printf("  %-6s %-6s %-6s %-10s %-20s%n",
                        o.getId(), o.getProductId(), o.getBuyerId(),
                        o.getStatus(), o.getCreateTime().toLocalDate());
            }
            ConsoleUtil.printLine();
            printOptions("确认订单", "拒绝订单", "返回");

            int choice = readChoice(1, 3);
            switch (choice) {
                case 1 -> {
                    String oid = InputUtil.readString("请输入订单ID：");
                    safeExecute(() -> {
                        orderService.confirmOrder(oid, currentUser.getId());
                        ConsoleUtil.printSuccess("订单已确认，等待买家收货");
                        InputUtil.pause();
                    });
                }
                case 2 -> {
                    String oid = InputUtil.readString("请输入订单ID：");
                    safeExecute(() -> {
                        orderService.rejectOrder(oid, currentUser.getId());
                        ConsoleUtil.printSuccess("订单已拒绝，款项已退给买家");
                        InputUtil.pause();
                    });
                }
                case 3 -> {
                    return;
                }
            }
        }
    }

    private void myReviews() {
        ConsoleUtil.printSubTitle("我收到的评价");
        List<Review> reviews = reviewService.listBySeller(currentUser.getId());
        if (reviews.isEmpty()) {
            ConsoleUtil.printWarning("暂无评价");
        } else {
            for (Review r : reviews) {
                System.out.println("  " + r);
            }
        }
        InputUtil.pause();
    }

    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 2) + ".." : s;
    }
}