package com.zhj.menu;

import com.zhj.model.Order;
import com.zhj.model.Product;
import com.zhj.model.Review;
import com.zhj.model.Seller;
import com.zhj.model.User;
import com.zhj.model.enums.Category;
import com.zhj.service.OrderService;
import com.zhj.service.ProductService;
import com.zhj.service.ReviewService;
import com.zhj.service.UserService;
import com.zhj.util.ConsoleUtil;
import com.zhj.util.InputUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 卖家菜单
 */
public class SellerMenu extends BaseMenu {

    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final ReviewService reviewService = new ReviewService();
    private final UserService userService = new UserService();

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

    /**
     * 我的商品 - 分页展示 + 操作
     */
    private void myProducts() {
        List<Product> products = productService.listProductsBySeller(currentUser.getId());
        int[] widths = {8, 24, 14, 12};

        Map<String, Runnable> extraActions = new HashMap<>();
        extraActions.put("1", this::editProduct);
        extraActions.put("2", this::offShelf);
        extraActions.put("3", this::reOnShelf);

        browsePaged(products, 5, "我的商品",
                pageData -> {
                    ConsoleUtil.printRow(widths, "ID", "名称", "价格", "状态");
                    ConsoleUtil.printLine();
                    for (Product p : pageData) {
                        ConsoleUtil.printRow(widths,
                                p.getId(),
                                truncate(p.getName(), 20),
                                String.format("￥%.2f", p.getPrice()),
                                p.getStatus().toString());
                    }
                },
                "[1]编辑 [2]下架 [3]重新上架",
                extraActions);
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

    /**
     * 订单管理 - 分页展示 + 操作
     */
    private void orderManagement() {
        List<Order> orders = orderService.listOrdersBySeller(currentUser.getId());
        int[] widths = {10, 24, 14, 10, 14};

        Map<String, Runnable> extraActions = new HashMap<>();
        extraActions.put("1", () -> {
            String oid = InputUtil.readString("请输入订单ID：");
            safeExecute(() -> {
                orderService.confirmOrder(oid, currentUser.getId());
                ConsoleUtil.printSuccess("订单已确认，等待买家收货");
                InputUtil.pause();
            });
        });
        extraActions.put("2", () -> {
            String oid = InputUtil.readString("请输入订单ID：");
            safeExecute(() -> {
                orderService.rejectOrder(oid, currentUser.getId());
                ConsoleUtil.printSuccess("订单已拒绝，款项已退给买家");
                InputUtil.pause();
            });
        });

        browsePaged(orders, 5, "我收到的订单",
                pageData -> {
                    ConsoleUtil.printRow(widths, "订单ID", "商品名称", "买家", "状态", "下单时间");
                    ConsoleUtil.printLine();
                    for (Order o : pageData) {
                        Product p = productService.getProductById(o.getProductId());
                        String pName = (p != null) ? truncate(p.getName(), 20) : o.getProductId();
                        User buyer = userService.getUserById(o.getBuyerId());
                        String buyerName = (buyer != null) ? buyer.getUsername() : o.getBuyerId();
                        ConsoleUtil.printRow(widths,
                                o.getId(),
                                pName,
                                buyerName,
                                o.getStatus().toString(),
                                o.getCreateTime().toLocalDate().toString());
                    }
                },
                "[1]确认订单 [2]拒绝订单",
                extraActions);
    }

    /**
     * 我收到的评价 - 分页展示
     */
    private void myReviews() {
        List<Review> reviews = reviewService.listBySeller(currentUser.getId());
        browsePaged(reviews, 10, "我收到的评价",
                pageData -> pageData.forEach(r -> System.out.println("  " + r)));
    }

    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 2) + ".." : s;
    }
}