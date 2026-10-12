package com.zhj.exp1.menu;

import com.zhj.exp1.model.Buyer;
import com.zhj.exp1.model.Order;
import com.zhj.exp1.model.Product;
import com.zhj.exp1.model.Review;
import com.zhj.exp1.model.User;
import com.zhj.exp1.model.enums.Category;
import com.zhj.exp1.model.enums.OrderStatus;
import com.zhj.exp1.service.OrderService;
import com.zhj.exp1.service.ProductService;
import com.zhj.exp1.service.ReviewService;
import com.zhj.exp1.strategy.*;
import com.zhj.exp1.util.ConsoleUtil;
import com.zhj.exp1.util.InputUtil;

import java.util.List;

/**
 * 买家菜单
 */
public class BuyerMenu extends BaseMenu {

    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final ReviewService reviewService = new ReviewService();

    public BuyerMenu(User user) {
        super(user);
    }

    private Buyer getBuyer() {
        return (Buyer) currentUser;
    }

    @Override
    public void show() {
        while (true) {
            ConsoleUtil.printTitle("买家菜单 - " + currentUser.getUsername()
                    + "  余额：￥" + String.format("%.2f", getBuyer().getBalance()));
            printOptions("浏览商品", "搜索商品", "按分类浏览", "我的订单", "我的评价", "返回主菜单");

            int choice = readChoice(1, 6);
            switch (choice) {
                case 1 -> browseWithSort();
                case 2 -> searchProducts();
                case 3 -> browseByCategory();
                case 4 -> myOrders();
                case 5 -> myReviews();
                case 6 -> {
                    logout();
                    return;
                }
            }
        }
    }

    /**
     * 浏览在售商品（可选排序，分页显示）
     */
    private void browseWithSort() {
        ConsoleUtil.printSubTitle("浏览商品 - 请选择排序方式");
        printOptions("按发布时间（最新）", "按价格从低到高", "按价格从高到低", "按热度（浏览量）", "默认顺序");

        int choice = readChoice(1, 5);
        List<Product> products = productService.listOnSaleProducts();
        SortStrategy strategy = switch (choice) {
            case 1 -> new SortByTimeDesc();
            case 2 -> new SortByPriceAsc();
            case 3 -> new SortByPriceDesc();
            case 4 -> new SortByHeatDesc();
            default -> null;
        };
        if (strategy != null) {
            products = productService.sortProducts(products, strategy);
        }
        showPaginatedProducts(products);
    }

    private void browseByCategory() {
        ConsoleUtil.printSubTitle("按分类浏览");
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("  [%d] %s%n", i + 1, categories[i]);
        }
        ConsoleUtil.printLine();

        int choice = InputUtil.readInt("请选择分类：", 1, categories.length);
        List<Product> products = productService.listOnSaleByCategory(categories[choice - 1]);
        showPaginatedProducts(products);
    }

    private void searchProducts() {
        ConsoleUtil.printSubTitle("搜索商品");
        String keyword = InputUtil.readString("请输入关键词：");
        List<Product> products = productService.searchOnSaleProducts(keyword);
        if (products.isEmpty()) {
            ConsoleUtil.printWarning("没有找到匹配的商品");
            InputUtil.pause();
            return;
        }
        showPaginatedProducts(products);
    }

    /**
     * 分页展示商品列表，允许翻页和查看详情
     */
    private void showPaginatedProducts(List<Product> products) {
        if (products.isEmpty()) {
            ConsoleUtil.printWarning("暂无商品");
            InputUtil.pause();
            return;
        }

        int page = 1;
        int pageSize = 5;   // 命令行显示，一页5条更清晰

        while (true) {
            ProductService.PageResult<Product> result = productService.paginate(products, page, pageSize);
            ConsoleUtil.printSubTitle("商品列表 - 第 " + result.currentPage + "/" + result.totalPages
                    + " 页（共 " + result.total + " 条）");
            System.out.printf("  %-6s %-8s %-22s %-8s %-8s%n", "ID", "分类", "名称", "价格", "状态");
            ConsoleUtil.printLine();
            for (Product p : result.data) {
                System.out.printf("  %-6s %-8s %-22s ￥%-7.2f %-8s%n",
                        p.getId(), p.getCategory(), truncate(p.getName(), 20),
                        p.getPrice(), p.getStatus());
            }
            ConsoleUtil.printLine();

            // 翻页菜单
            StringBuilder sb = new StringBuilder();
            if (result.hasPrev()) sb.append("[p]上一页 ");
            if (result.hasNext()) sb.append("[n]下一页 ");
            sb.append("[v]查看详情 [q]返回");
            System.out.println("  " + sb);

            String op = InputUtil.readString("请输入操作：").toLowerCase();
            switch (op) {
                case "n" -> {
                    if (result.hasNext()) page++;
                }
                case "p" -> {
                    if (result.hasPrev()) page--;
                }
                case "v" -> viewProductDetail();
                case "q" -> {
                    return;
                }
                default -> ConsoleUtil.printWarning("无效操作");
            }
        }
    }

    private void viewProductDetail() {
        String pid = InputUtil.readString("请输入商品ID：");
        safeExecute(() -> {
            Product p = productService.viewProductDetail(pid);
            ConsoleUtil.printSubTitle("商品详情");
            System.out.println("  ID：       " + p.getId());
            System.out.println("  名称：     " + p.getName());
            System.out.println("  分类：     " + p.getCategory());
            System.out.println("  价格：     ￥" + String.format("%.2f", p.getPrice()));
            System.out.println("  状态：     " + p.getStatus());
            System.out.println("  浏览量：   " + p.getViewCount());
            System.out.println("  描述：     " + p.getDescription());
            System.out.println("  卖家ID：   " + p.getSellerId());
            ConsoleUtil.printLine();

            String op = InputUtil.readString("是否下单购买？(y/n)：").toLowerCase();
            if (op.equals("y")) {
                safeExecute(() -> {
                    Order order = orderService.createOrder(currentUser.getId(), pid);
                    ConsoleUtil.printSuccess("下单成功！订单号：" + order.getId()
                            + "，等待卖家确认。已从你的余额中扣款 ￥" + String.format("%.2f", p.getPrice()));
                    InputUtil.pause();
                });
            }
        });
    }

    private void myOrders() {
        while (true) {
            ConsoleUtil.printSubTitle("我的订单");
            List<Order> orders = orderService.listOrdersByBuyer(currentUser.getId());
            if (orders.isEmpty()) {
                ConsoleUtil.printWarning("你还没有任何订单");
                InputUtil.pause();
                return;
            }
            System.out.printf("  %-6s %-6s %-10s %-20s%n", "订单ID", "商品ID", "状态", "下单时间");
            ConsoleUtil.printLine();
            for (Order o : orders) {
                System.out.printf("  %-6s %-6s %-10s %-20s%n",
                        o.getId(), o.getProductId(), o.getStatus(),
                        o.getCreateTime().toLocalDate());
            }
            ConsoleUtil.printLine();
            printOptions("取消订单", "确认收货", "评价订单", "返回");

            int choice = readChoice(1, 4);
            switch (choice) {
                case 1 -> {
                    String oid = InputUtil.readString("请输入订单ID：");
                    safeExecute(() -> {
                        orderService.cancelOrder(oid, currentUser.getId());
                        ConsoleUtil.printSuccess("订单已取消，退款已到账");
                        InputUtil.pause();
                    });
                }
                case 2 -> {
                    String oid = InputUtil.readString("请输入订单ID：");
                    safeExecute(() -> {
                        orderService.receiveOrder(oid, currentUser.getId());
                        ConsoleUtil.printSuccess("已确认收货");
                        InputUtil.pause();
                    });
                }
                case 3 -> {
                    String oid = InputUtil.readString("请输入订单ID：");
                    int rating = InputUtil.readInt("评分(1-5)：", 1, 5);
                    String content = InputUtil.readString("评价内容：");
                    safeExecute(() -> {
                        reviewService.addReview(currentUser.getId(), oid, rating, content);
                        ConsoleUtil.printSuccess("评价成功，感谢你的反馈！");
                        InputUtil.pause();
                    });
                }
                case 4 -> {
                    return;
                }
            }
        }
    }

    private void myReviews() {
        ConsoleUtil.printSubTitle("我发出的评价");
        List<Review> reviews = reviewService.listByBuyer(currentUser.getId());
        if (reviews.isEmpty()) {
            ConsoleUtil.printWarning("你还没有评价过任何订单");
        } else {
            for (Review r : reviews) {
                System.out.println("  " + r);
            }
        }
        InputUtil.pause();
    }

    /**
     * 截断过长字符串，保持表格整齐
     */
    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 2) + ".." : s;
    }
}