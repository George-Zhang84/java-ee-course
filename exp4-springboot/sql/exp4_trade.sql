-- =============================================================
-- 实验四 校园二手商品交易管理系统  数据库脚本 exp4_trade.sql
-- 适用：MySQL 8.0+，字符集 utf8mb4
-- 说明：本脚本会 DROP 并重建 exp4_trade 库中的表，仅用于本地/测试环境。
--      与实验三库 exp3_trade_rbac 完全独立，互不影响。
-- 表清单（16 张）：
--   RBAC：sys_user / sys_role / sys_user_role / sys_permission / sys_role_permission
--   业务：category / product / product_image / favorite / `order` / bargain_offer
--         review / wallet_log / notification
--   运维：audit_log / login_log
-- 设计要点：
--   1) 校园二手为"单件交易"，故订单表一件商品一行，不设 order_item（避免过度设计）；
--   2) 金额统一 DECIMAL，禁止 float；所有业务表带 create_time 便于统计与排查；
--   3) product.status 为状态机字段（PENDING/ON_SALE/LOCKED/SOLD/REJECTED/OFF_SHELF）；
--   4) 密码使用 BCrypt($2a$10)，演示账号密码均为 123456。
-- =============================================================

DROP DATABASE IF EXISTS `exp4_trade`;
CREATE DATABASE `exp4_trade` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `exp4_trade`;
SET NAMES utf8mb4;

-- -------------------------------------------------------------
-- 1. 用户表
-- -------------------------------------------------------------
CREATE TABLE `sys_user` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`     VARCHAR(32)   NOT NULL                COMMENT '登录名',
  `password`     VARCHAR(100)  NOT NULL                COMMENT 'BCrypt 密文',
  `nickname`     VARCHAR(32)   NOT NULL                COMMENT '昵称',
  `phone`        VARCHAR(20)            DEFAULT NULL   COMMENT '手机号',
  `email`        VARCHAR(64)            DEFAULT NULL   COMMENT '邮箱',
  `avatar`       VARCHAR(255)           DEFAULT NULL   COMMENT '头像 URL',
  `balance`      DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '钱包余额',
  `credit_score` INT           NOT NULL DEFAULT 100    COMMENT '信用分（评价动态调整）',
  `status`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1 正常 0 禁用',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='用户表';

-- -------------------------------------------------------------
-- 2. 角色表 / 3. 用户-角色 / 4. 权限表 / 5. 角色-权限（标准 RBAC）
-- -------------------------------------------------------------
CREATE TABLE `sys_role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(32) NOT NULL COMMENT '角色编码',
  `name`        VARCHAR(32) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(128)         DEFAULT NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE `sys_user_role` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB COMMENT='用户角色关联';

CREATE TABLE `sys_permission` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `code`      VARCHAR(64) NOT NULL COMMENT '权限码，如 product:publish',
  `name`      VARCHAR(64) NOT NULL,
  `type`      VARCHAR(16) NOT NULL COMMENT 'MENU/BUTTON/API',
  `parent_id` BIGINT      NOT NULL DEFAULT 0,
  `path`      VARCHAR(128)         DEFAULT NULL COMMENT '前端路由或接口路径',
  `icon`      VARCHAR(32)          DEFAULT NULL,
  `sort`      INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='权限表';

CREATE TABLE `sys_role_permission` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `role_id`       BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`),
  KEY `idx_perm` (`permission_id`)
) ENGINE=InnoDB COMMENT='角色权限关联';

-- -------------------------------------------------------------
-- 6. 商品分类
-- -------------------------------------------------------------
CREATE TABLE `category` (
  `id`     BIGINT      NOT NULL AUTO_INCREMENT,
  `name`   VARCHAR(32) NOT NULL,
  `icon`   VARCHAR(64)          DEFAULT NULL,
  `sort`   INT         NOT NULL DEFAULT 0,
  `status` TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB COMMENT='商品分类';

-- -------------------------------------------------------------
-- 7. 商品
-- -------------------------------------------------------------
CREATE TABLE `product` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `seller_id`     BIGINT        NOT NULL COMMENT '卖家用户 id',
  `category_id`   BIGINT        NOT NULL COMMENT '分类 id',
  `title`         VARCHAR(64)   NOT NULL COMMENT '标题',
  `description`   VARCHAR(1000)          DEFAULT NULL,
  `price`         DECIMAL(10,2) NOT NULL COMMENT '售价',
  `min_price`     DECIMAL(10,2)          DEFAULT NULL COMMENT '卖家底价，仅参与自动成交判断，不对外返回',
  `allow_bargain` TINYINT       NOT NULL DEFAULT 1 COMMENT '1 允许议价',
  `condition_level` VARCHAR(16)          DEFAULT '99新' COMMENT '成色',
  `location`      VARCHAR(64)            DEFAULT NULL COMMENT '交易地点',
  `cover_url`     VARCHAR(255)           DEFAULT NULL COMMENT '封面图',
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'PENDING'
                  COMMENT 'PENDING 待审核 / ON_SALE 在售 / LOCKED 已锁定 / SOLD 已售出 / REJECTED 驳回 / OFF_SHELF 下架',
  `audit_remark`  VARCHAR(255)           DEFAULT NULL COMMENT '审核意见',
  `views`         INT           NOT NULL DEFAULT 0 COMMENT '浏览量',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status_cate` (`status`,`category_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_price` (`price`),
  KEY `idx_create` (`create_time`)
) ENGINE=InnoDB COMMENT='商品表';

CREATE TABLE `product_image` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT       NOT NULL,
  `url`        VARCHAR(255) NOT NULL,
  `sort`       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='商品多图';

-- -------------------------------------------------------------
-- 收藏夹（同一用户对同一商品仅一条，唯一索引天然幂等）
-- -------------------------------------------------------------
CREATE TABLE `favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `product_id`  BIGINT   NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='收藏夹';

-- -------------------------------------------------------------
-- 8. 订单（单件交易，一件商品一行）
-- -------------------------------------------------------------
CREATE TABLE `order` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`      VARCHAR(32)   NOT NULL COMMENT '业务单号',
  `product_id`    BIGINT        NOT NULL,
  `buyer_id`      BIGINT        NOT NULL,
  `seller_id`     BIGINT        NOT NULL,
  `amount`        DECIMAL(10,2) NOT NULL COMMENT '成交金额（原价或议价）',
  `origin`        VARCHAR(16)   NOT NULL DEFAULT 'DIRECT' COMMENT 'DIRECT 直接购买 / BARGAIN 议价成交',
  `offer_id`      BIGINT                 DEFAULT NULL COMMENT '议价成交对应的报价单',
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'UNPAID'
                  COMMENT 'UNPAID / PAID / SHIPPED / COMPLETED / CANCELLED',
  `pay_time`      DATETIME               DEFAULT NULL,
  `ship_time`     DATETIME               DEFAULT NULL,
  `finish_time`   DATETIME               DEFAULT NULL,
  `cancel_reason` VARCHAR(255)           DEFAULT NULL,
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_offer` (`offer_id`),
  KEY `idx_buyer_status` (`buyer_id`,`status`),
  KEY `idx_seller_status` (`seller_id`,`status`),
  KEY `idx_status_create` (`status`,`create_time`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB COMMENT='订单表';

-- -------------------------------------------------------------
-- 9. 议价报价单（特色功能 S1）
-- -------------------------------------------------------------
CREATE TABLE `bargain_offer` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `product_id`   BIGINT        NOT NULL,
  `buyer_id`     BIGINT        NOT NULL,
  `seller_id`    BIGINT        NOT NULL,
  `offer_price`  DECIMAL(10,2) NOT NULL COMMENT '买家出价',
  `message`      VARCHAR(255)           DEFAULT NULL COMMENT '买家留言',
  `status`       VARCHAR(16)   NOT NULL DEFAULT 'PENDING'
                 COMMENT 'PENDING / ACCEPTED / REJECTED / CANCELLED / EXPIRED',
  `expire_time`  DATETIME      NOT NULL COMMENT '过期时间（创建 +24h）',
  `handled_time` DATETIME               DEFAULT NULL COMMENT '卖家处理时间',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_status` (`product_id`,`status`),
  KEY `idx_seller_status` (`seller_id`,`status`),
  KEY `idx_buyer` (`buyer_id`),
  KEY `idx_expire` (`status`,`expire_time`)
) ENGINE=InnoDB COMMENT='议价报价单';

-- -------------------------------------------------------------
-- 10. 评价
-- -------------------------------------------------------------
CREATE TABLE `review` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `order_id`      BIGINT       NOT NULL,
  `product_id`    BIGINT       NOT NULL,
  `reviewer_id`   BIGINT       NOT NULL,
  `reviewee_id`   BIGINT       NOT NULL,
  `reviewer_role` VARCHAR(8)   NOT NULL COMMENT 'BUYER 买家评卖家 / SELLER 卖家评买家',
  `score`         TINYINT      NOT NULL COMMENT '1~5 星',
  `content`       VARCHAR(500)          DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_reviewer` (`order_id`,`reviewer_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_reviewee` (`reviewee_id`)
) ENGINE=InnoDB COMMENT='订单评价';

-- -------------------------------------------------------------
-- 11. 钱包流水
-- -------------------------------------------------------------
CREATE TABLE `wallet_log` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT        NOT NULL,
  `type`          VARCHAR(16)   NOT NULL COMMENT 'RECHARGE 充值 / PAY 支付 / INCOME 卖家入账 / REFUND 退款',
  `amount`        DECIMAL(12,2) NOT NULL COMMENT '正数收入，负数支出',
  `balance_after` DECIMAL(12,2) NOT NULL COMMENT '变动后余额（对账用）',
  `biz_type`      VARCHAR(32)            DEFAULT NULL COMMENT '关联业务类型',
  `biz_id`        BIGINT                 DEFAULT NULL COMMENT '关联业务 id',
  `remark`        VARCHAR(255)           DEFAULT NULL,
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_create` (`user_id`,`create_time`)
) ENGINE=InnoDB COMMENT='资金流水';

-- -------------------------------------------------------------
-- 12. 站内通知
-- -------------------------------------------------------------
CREATE TABLE `notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `type`        VARCHAR(32)  NOT NULL COMMENT 'ORDER / BARGAIN / AUDIT / SYSTEM',
  `title`       VARCHAR(64)  NOT NULL,
  `content`     VARCHAR(255)          DEFAULT NULL,
  `biz_id`      BIGINT                DEFAULT NULL,
  `read_flag`   TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未读 1 已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`read_flag`)
) ENGINE=InnoDB COMMENT='站内通知';

-- -------------------------------------------------------------
-- 13. 操作审计日志（AOP 切面写入）
-- -------------------------------------------------------------
CREATE TABLE `audit_log` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT               DEFAULT NULL,
  `username`    VARCHAR(32)          DEFAULT NULL,
  `module`      VARCHAR(32)  NOT NULL COMMENT '业务模块',
  `action`      VARCHAR(64)  NOT NULL COMMENT '动作描述',
  `method`      VARCHAR(128)          DEFAULT NULL COMMENT 'Java 方法',
  `uri`         VARCHAR(255)          DEFAULT NULL,
  `ip`          VARCHAR(64)           DEFAULT NULL,
  `params`      TEXT                  COMMENT '入参摘要（脱敏后）',
  `result`      VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAIL',
  `cost_ms`     INT          NOT NULL DEFAULT 0,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_create` (`create_time`),
  KEY `idx_user` (`user_id`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB COMMENT='操作审计日志';

-- -------------------------------------------------------------
-- 14. 登录日志
-- -------------------------------------------------------------
CREATE TABLE `login_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT                DEFAULT NULL,
  `username`    VARCHAR(32)  NOT NULL,
  `ip`          VARCHAR(64)           DEFAULT NULL,
  `user_agent`  VARCHAR(255)          DEFAULT NULL,
  `success`     TINYINT      NOT NULL DEFAULT 1,
  `msg`         VARCHAR(255)          DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_create` (`username`,`create_time`),
  KEY `idx_create` (`create_time`)
) ENGINE=InnoDB COMMENT='登录日志';

-- =============================================================
-- 初始化数据
-- =============================================================
-- 角色
INSERT INTO `sys_role` (`id`,`code`,`name`,`description`) VALUES
 (1,'SUPER_ADMIN','超级管理员','拥有全部权限（31 项），可管理角色与权限矩阵'),
 (2,'ADMIN','管理员','审核商品、管理用户、查看看板与审计（12 项，不含个人买卖功能）'),
 (3,'USER','普通用户','人人可买可卖：浏览、下单、发布商品、议价、钱包、评价（20 项）'),
 (4,'SELLER','认证卖家','仅作"已认证店铺"身份标识，功能权限与 USER 完全一致（20 项）；保留用于后续扩展（优先展示、保证金、店铺主页等）');

-- 权限（menu 菜单 / btn 前端按钮 / api 接口）
INSERT INTO `sys_permission` (`id`,`code`,`name`,`type`,`parent_id`,`path`,`icon`,`sort`) VALUES
 ( 1,'menu:home',        '首页',       'MENU',0,'/home',        'HomeFilled', 1),
 ( 2,'menu:product',     '商品市场',   'MENU',0,'/products',    'Goods',      2),
 ( 3,'menu:order',       '我的订单',   'MENU',0,'/orders',      'List',       3),
 ( 4,'menu:bargain',     '议价消息',   'MENU',0,'/bargains',    'ChatDotRound',4),
 ( 5,'menu:wallet',      '我的钱包',   'MENU',0,'/wallet',      'Wallet',     5),
 ( 6,'menu:profile',     '个人中心',   'MENU',0,'/profile',     'User',       6),
 ( 7,'menu:mine',        '我的商品',   'MENU',0,'/my-products', 'Sell',       7),
 ( 8,'menu:admin:pending','商品审核',  'MENU',0,'/admin/pending','Checked',   8),
 ( 9,'menu:admin:user',  '用户管理',   'MENU',0,'/admin/users', 'UserFilled', 9),
 (10,'menu:admin:role',  '角色权限',   'MENU',0,'/admin/roles', 'Setting',   10),
 (11,'menu:admin:audit', '审计日志',   'MENU',0,'/admin/audit', 'Document',  11),
 (12,'menu:admin:dashboard','数据看板','MENU',0,'/admin/dashboard','DataLine',12),
 (13,'product:publish',  '发布商品',   'BUTTON',0,NULL,NULL,20),
 (14,'product:edit',     '编辑商品',   'BUTTON',0,NULL,NULL,21),
 (15,'product:offline',  '下架商品',   'BUTTON',0,NULL,NULL,22),
 (16,'product:audit',    '审核商品',   'BUTTON',0,NULL,NULL,23),
 (17,'product:force-offline','强制下架','BUTTON',0,NULL,NULL,24),
 (18,'order:pay',        '支付订单',   'BUTTON',0,NULL,NULL,30),
 (19,'order:ship',       '订单发货',   'BUTTON',0,NULL,NULL,31),
 (20,'order:confirm',    '确认收货',   'BUTTON',0,NULL,NULL,32),
 (21,'order:cancel',     '取消订单',   'BUTTON',0,NULL,NULL,33),
 (22,'bargain:offer',    '发起还价',   'BUTTON',0,NULL,NULL,40),
 (23,'bargain:handle',   '处理还价',   'BUTTON',0,NULL,NULL,41),
 (24,'wallet:recharge',  '钱包充值',   'BUTTON',0,NULL,NULL,50),
 (25,'review:create',    '发表评价',   'BUTTON',0,NULL,NULL,60),
 (26,'user:manage',      '用户管理',   'API',   0,NULL,NULL,70),
 (27,'role:manage',      '角色管理',   'API',   0,NULL,NULL,71),
 (28,'audit:view',       '审计查看',   'API',   0,NULL,NULL,72),
 (29,'dashboard:view',   '看板查看',   'API',   0,NULL,NULL,73),
 (30,'menu:favorites',   '我的收藏',   'MENU',  0,'/favorites',    'Star',13),
 (31,'menu:message',     '站内消息',   'MENU',  0,'/notifications','Bell',14);

-- 角色-权限
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
-- SUPER_ADMIN：全部 31 项（含菜单 12 + 按钮/接口 19）
 (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),
 (1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,21),(1,22),(1,23),
 (1,24),(1,25),(1,26),(1,27),(1,28),(1,29),(1,30),(1,31),
-- ADMIN：审核 + 用户 + 看板 + 审计（不含角色管理）
 (2,1),(2,2),(2,6),(2,8),(2,9),(2,11),(2,12),(2,16),(2,17),(2,26),(2,28),(2,29),
-- USER：校园场景下"人人可买可卖"——浏览/下单/议价/钱包/评价 + 发布/编辑/下架商品 + 发货 + 处理还价
 (3,1),(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),(3,13),(3,14),(3,15),(3,18),(3,19),(3,20),
 (3,21),(3,22),(3,23),(3,24),(3,25),(3,30),(3,31),
-- SELLER：校园"认证卖家"标识（右上角徽章）。功能权限与 USER 一致——平台不限制普通用户卖东西，
--         SELLER 仅用于标识"已认证/有店铺"，便于后续扩展（如优先展示、保证金等）
 (4,1),(4,2),(4,3),(4,4),(4,5),(4,6),(4,7),(4,13),(4,14),(4,15),(4,18),(4,19),
 (4,20),(4,21),(4,22),(4,23),(4,24),(4,25),(4,30),(4,31);

-- 用户（密码均为 123456，BCrypt cost=10）
INSERT INTO `sys_user` (`id`,`username`,`password`,`nickname`,`phone`,`email`,`balance`,`credit_score`,`status`) VALUES
 (1,'admin',   '$2a$10$ZzHFJC.VzjTfsCEmwNRMduoem4jzutNjtBpshcDRpQCF46m3.dwCC','超级管理员','13800000001','admin@campus.edu',   0.00,100,1),
 (2,'manager', '$2a$10$ZzHFJC.VzjTfsCEmwNRMduoem4jzutNjtBpshcDRpQCF46m3.dwCC','管理员',   '13800000002','manager@campus.edu', 0.00,100,1),
 (3,'buyer1',  '$2a$10$ZzHFJC.VzjTfsCEmwNRMduoem4jzutNjtBpshcDRpQCF46m3.dwCC','小明',     '13800000003','buyer1@campus.edu',1200.00,105,1),
 (4,'buyer2',  '$2a$10$ZzHFJC.VzjTfsCEmwNRMduoem4jzutNjtBpshcDRpQCF46m3.dwCC','小红',     '13800000004','buyer2@campus.edu', 500.00,100,1),
 (5,'seller1', '$2a$10$ZzHFJC.VzjTfsCEmwNRMduoem4jzutNjtBpshcDRpQCF46m3.dwCC','数码小铺', '13800000005','seller1@campus.edu', 240.00,102,1),
 (6,'seller2', '$2a$10$ZzHFJC.VzjTfsCEmwNRMduoem4jzutNjtBpshcDRpQCF46m3.dwCC','旧书摊',   '13800000006','seller2@campus.edu',  15.00, 98,1);

-- 用户-角色：buyer 只有 USER；seller 同时具备 USER + SELLER（体现多角色 RBAC）
INSERT INTO `sys_user_role` (`user_id`,`role_id`) VALUES
 (1,1),(2,2),(3,3),(4,3),(5,3),(5,4),(6,3),(6,4);

-- 分类
INSERT INTO `category` (`id`,`name`,`icon`,`sort`,`status`) VALUES
 (1,'电子数码','Cellphone',1,1),
 (2,'教材书籍','Reading',2,1),
 (3,'运动户外','Basketball',3,1),
 (4,'生活用品','House',4,1),
 (5,'服饰鞋包','ShoppingBag',5,1),
 (6,'其他','MoreFilled',6,1);

-- 商品
INSERT INTO `product` (`id`,`seller_id`,`category_id`,`title`,`description`,`price`,`min_price`,`allow_bargain`,`condition_level`,`location`,`cover_url`,`status`,`audit_remark`,`views`) VALUES
 (1,5,1,'罗技无线鼠标 M330','静音版，99新，换新鼠标了所以出掉。',89.00,70.00,1,'99新','三号宿舍楼下','/uploads/demo/mouse.jpg','LOCKED',NULL,36),
 (2,5,1,'机械键盘 青轴 87 键','手感极佳，吃灰很久了，可小刀。',200.00,170.00,1,'95新','图书馆门口','/uploads/demo/keyboard.jpg','ON_SALE',NULL,88),
 (3,6,2,'考研英语真题卷 2025','全新未写，买错版本了。',15.00,NULL,0,'全新','二食堂','/uploads/demo/book1.jpg','ON_SALE',NULL,12),
 (4,6,2,'Java 核心技术 卷1','书角有轻微磨损，内页无笔记。',50.00,40.00,1,'9成新','二食堂','/uploads/demo/book2.jpg','PENDING',NULL,0),
 (5,5,3,'阿迪达斯篮球','打了两次，手感很好。',120.00,100.00,1,'95新','东区球场','/uploads/demo/ball.jpg','SOLD',NULL,45),
 (6,6,4,'捷安特山地车 26 寸','毕业出，车况良好，可小刀。',300.00,250.00,1,'8成新','南门车棚','/uploads/demo/bike.jpg','ON_SALE',NULL,64);

INSERT INTO `product_image` (`product_id`,`url`,`sort`) VALUES
 (2,'/uploads/demo/keyboard.jpg',0),
 (6,'/uploads/demo/bike.jpg',0);

-- 订单：1 笔已完成（评价用），1 笔超时未支付（定时任务演示用，创建于 20 分钟前）
INSERT INTO `order` (`id`,`order_no`,`product_id`,`buyer_id`,`seller_id`,`amount`,`origin`,`offer_id`,`status`,`pay_time`,`ship_time`,`finish_time`,`create_time`) VALUES
 (1,'ORD202512200001',5,3,5,120.00,'DIRECT',NULL,'COMPLETED','2025-12-20 10:01:00','2025-12-20 14:00:00','2025-12-21 09:00:00','2025-12-20 10:00:00'),
 (2,'ORD202512210001',1,4,5,89.00,'DIRECT',NULL,'UNPAID',NULL,NULL,NULL, NOW() - INTERVAL 20 MINUTE);

-- 议价单：buyer2 对商品 2 出价 180，等待卖家处理
INSERT INTO `bargain_offer` (`id`,`product_id`,`buyer_id`,`seller_id`,`offer_price`,`message`,`status`,`expire_time`,`create_time`) VALUES
 (1,2,4,5,180.00,'能便宜 20 吗？可以马上来自提。','PENDING', NOW() + INTERVAL 24 HOUR, NOW());

-- 评价：订单 1 买家评卖家
INSERT INTO `review` (`order_id`,`product_id`,`reviewer_id`,`reviewee_id`,`reviewer_role`,`score`,`content`) VALUES
 (1,5,3,5,'BUYER',5,'球很新，卖家爽快，约在球场自提很方便。');

-- 钱包流水
INSERT INTO `wallet_log` (`user_id`,`type`,`amount`,`balance_after`,`biz_type`,`biz_id`,`remark`) VALUES
 (3,'RECHARGE', 1300.00,1300.00,'RECHARGE',NULL,'演示充值'),
 (3,'PAY',      -120.00,1180.00,'ORDER',1,'支付订单 ORD202512200001'),
 (5,'INCOME',    120.00, 120.00,'ORDER',1,'订单 ORD202512200001 结算入账');

-- 站内通知
INSERT INTO `notification` (`user_id`,`type`,`title`,`content`,`biz_id`,`read_flag`) VALUES
 (3,'ORDER','订单已完成','订单 ORD202512200001 已确认收货，欢迎评价。',1,1),
 (5,'BARGAIN','收到新的还价','买家 小红 对「机械键盘 青轴 87 键」出价 180 元。',1,0);

-- 审计/登录日志由系统运行时写入，此处仅给出示例
INSERT INTO `login_log` (`user_id`,`username`,`ip`,`user_agent`,`success`,`msg`) VALUES
 (3,'buyer1','127.0.0.1','Mozilla/5.0 (Windows NT 10.0; Win64; x64)',1,'登录成功');
INSERT INTO `audit_log` (`user_id`,`username`,`module`,`action`,`method`,`uri`,`ip`,`params`,`result`,`cost_ms`) VALUES
 (5,'seller1','商品','上架商品','ProductService.offline','PUT /api/products/2/status','127.0.0.1','{"status":"ON_SALE"}','SUCCESS',23);
