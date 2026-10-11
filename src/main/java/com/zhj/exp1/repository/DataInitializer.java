package com.zhj.exp1.repository;

import com.zhj.exp1.model.Buyer;
import com.zhj.exp1.model.Product;
import com.zhj.exp1.model.Seller;
import com.zhj.exp1.model.Admin;
import com.zhj.exp1.model.enums.Category;
import com.zhj.exp1.model.enums.ProductStatus;
import com.zhj.exp1.util.IdGenerator;

import java.util.Random;

/**
 * 数据初始化器
 * 程序启动时调用一次，生成测试数据，方便演示和测试
 */
public class DataInitializer {

    private static final Random random = new Random();

    // 姓氏库
    private static final String[] SURNAMES = {
            "张", "王", "李", "赵", "刘", "陈", "杨", "黄", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "林", "何", "高", "罗"
    };

    // 名字库
    private static final String[] GIVEN_NAMES = {
            "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "洋", "艳",
            "勇", "军", "杰", "娟", "涛", "明", "超", "秀英", "霞", "平",
            "刚", "桂英", "华", "鹏", "宇", "晨", "轩", "雨", "欣", "悦"
    };

    // 店铺名库
    private static final String[] SHOP_PREFIXES = {
            "校园", "阳光", "青春", "学长", "学姐", "好物", "二手", "优品"
    };

    // 商品名库（按分类）
    private static final String[][] PRODUCT_NAMES = {
            // BOOK
            {"Java编程思想", "数据结构与算法分析", "计算机网络自顶向下", "操作系统概念",
                    "深入理解计算机系统", "算法导论", "数据库系统概论", "软件工程导论"},
            // ELECTRONICS
            {"iPad Air 4", "小米蓝牙耳机", "罗技无线鼠标", "联想笔记本电源",
                    "Kindle阅读器", "华为手环", "机械键盘", "显示器支架"},
            // DAILY
            {"宿舍台灯", "收纳箱", "保温杯", "电吹风", "洗衣液",
                    "床上桌", "挂钩套装", "雨伞"},
            // CLOTHING
            {"羽绒服", "运动鞋", "卫衣", "牛仔裤", "背包",
                    "棒球帽", "围巾", "手套"},
            // SPORTS
            {"篮球", "羽毛球拍", "瑜伽垫", "跳绳", "哑铃",
                    "足球", "网球拍", "滑板"},
            // OTHER
            {"吉他", "素描本", "自行车", "小风扇", "化妆品收纳盒",
                    "拼图", "水彩笔", "吉他调音器"}
    };

    // 商品描述库
    private static final String[] DESCRIPTIONS = {
            "九成新，无任何损坏，急出",
            "用了一个学期，功能完好",
            "原价购买，现低价转让",
            "毕业清仓，价格可小刀",
            "买来就没怎么用，几乎全新",
            "有轻微使用痕迹，不影响使用",
            "正品，可提供购买凭证",
            "宿舍搬家，低价处理"
    };

    /**
     * 生成初始数据（只调用一次）
     */
    public static void init() {
        DataStore store = DataStore.getInstance();

        // 1. 生成 3 个管理员
        for (int i = 0; i < 3; i++) {
            Admin admin = new Admin(
                    IdGenerator.nextUserId(),
                    "admin" + (i + 1),
                    "123456",
                    randomPhone()
            );
            admin.setAdminLevel(i == 0 ? 2 : 1);  // 第一个是超级管理员
            store.addUser(admin);
        }

        // 2. 生成 8 个卖家
        Seller[] sellers = new Seller[8];
        for (int i = 0; i < 8; i++) {
            Seller seller = new Seller(
                    IdGenerator.nextUserId(),
                    "seller" + (i + 1),
                    "123456",
                    randomPhone(),
                    SHOP_PREFIXES[i % SHOP_PREFIXES.length] + "小铺"
            );
            store.addUser(seller);
            sellers[i] = seller;
        }

        // 3. 生成 100 个买家
        Buyer[] buyers = new Buyer[100];
        for (int i = 0; i < 100; i++) {
            Buyer buyer = new Buyer(
                    IdGenerator.nextUserId(),
                    "buyer" + (i + 1),
                    "123456",
                    randomPhone()
            );
            buyer.setBalance(500 + random.nextInt(2000));   // 余额 500~2500
            store.addUser(buyer);
            buyers[i] = buyer;
        }

        // 4. 生成 30 个商品，随机分配给卖家
        for (int i = 0; i < 30; i++) {
            Category category = Category.values()[random.nextInt(Category.values().length)];
            String[] namePool = PRODUCT_NAMES[category.ordinal()];
            String name = namePool[random.nextInt(namePool.length)];
            double price = 10 + random.nextInt(500);   // 10~510 元

            Seller seller = sellers[random.nextInt(sellers.length)];

            Product product = new Product(
                    IdGenerator.nextProductId(),
                    name,
                    DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)],
                    price,
                    category,
                    seller.getId()
            );

            // 随机状态：60% 在售，20% 待审核，10% 已下架，10% 已售出
            int r = random.nextInt(10);
            if (r < 6) {
                product.setStatus(ProductStatus.ON_SALE);
            } else if (r < 8) {
                product.setStatus(ProductStatus.PENDING);
            } else if (r < 9) {
                product.setStatus(ProductStatus.OFF_SHELF);
            } else {
                product.setStatus(ProductStatus.SOLD);
            }
            product.setViewCount(random.nextInt(200));

            store.addProduct(product);
        }

        System.out.println("✔ 数据初始化完成：");
        System.out.printf("  用户 %d 个（管理员3 / 卖家8 / 买家100）%n", store.getUserCount());
        System.out.printf("  商品 %d 个%n", store.getProductCount());
    }

    /**
     * 生成随机手机号
     */
    private static String randomPhone() {
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}