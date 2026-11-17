-- ============================================================
-- 校园二手商品交易管理系统（Java 企业级应用 实验3）数据库初始化脚本
-- 适用版本：MySQL 8.0
-- 使用方式：mysql -uroot -p < exp3_trade_rbac.sql
-- 说明：包含建库、建表与演示数据；演示账号密码均为 123456
-- ============================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `exp3_trade_rbac` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `exp3_trade_rbac`;
DROP TABLE IF EXISTS `order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `buyer_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `seller_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `buyer_id` (`buyer_id`),
  KEY `seller_id` (`seller_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `order_ibfk_1` FOREIGN KEY (`buyer_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `order_ibfk_2` FOREIGN KEY (`seller_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `order_ibfk_3` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `seller_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `views` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `seller_id` (`seller_id`),
  CONSTRAINT `product_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `buyer_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `seller_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `rating` int NOT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int NOT NULL DEFAULT '1' COMMENT '1正常, 0已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `order_id` (`order_id`),
  CONSTRAINT `review_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `parent_id` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `permission_id` int NOT NULL,
  PRIMARY KEY (`role_code`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `balance` decimal(10,2) DEFAULT '0.00',
  `shop_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` int DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `username`, `password`, `role_code`, `balance`, `shop_name`, `status`, `create_time`) VALUES ('U001','admin','e10adc3949ba59abbe56e057f20f883e','SUPER_ADMIN',0.00,NULL,1,'2026-11-17 08:17:22');
INSERT INTO `sys_user` (`id`, `username`, `password`, `role_code`, `balance`, `shop_name`, `status`, `create_time`) VALUES ('U002','manager','e10adc3949ba59abbe56e057f20f883e','ADMIN',0.00,NULL,1,'2026-11-17 08:17:22');
INSERT INTO `sys_user` (`id`, `username`, `password`, `role_code`, `balance`, `shop_name`, `status`, `create_time`) VALUES ('U003','buyer1','e10adc3949ba59abbe56e057f20f883e','BUYER',1000.00,NULL,1,'2026-11-17 08:17:22');
INSERT INTO `sys_user` (`id`, `username`, `password`, `role_code`, `balance`, `shop_name`, `status`, `create_time`) VALUES ('U004','buyer2','e10adc3949ba59abbe56e057f20f883e','BUYER',500.00,NULL,1,'2026-11-17 08:17:22');
INSERT INTO `sys_user` (`id`, `username`, `password`, `role_code`, `balance`, `shop_name`, `status`, `create_time`) VALUES ('U005','seller1','e10adc3949ba59abbe56e057f20f883e','SELLER',0.00,'数码小铺',1,'2026-11-17 08:17:22');
INSERT INTO `sys_user` (`id`, `username`, `password`, `role_code`, `balance`, `shop_name`, `status`, `create_time`) VALUES ('U006','seller2','e10adc3949ba59abbe56e057f20f883e','SELLER',0.00,'旧书摊',1,'2026-11-17 08:17:22');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`) VALUES (1,'menu:product','商品管理菜单','MENU',0);
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`) VALUES (2,'menu:order','订单管理菜单','MENU',0);
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`) VALUES (3,'menu:user','用户管理菜单','MENU',0);
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`) VALUES (4,'menu:audit','商品审核菜单','MENU',0);
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`) VALUES (5,'product:audit','审核商品操作','BUTTON',0);
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`) VALUES (6,'product:offline','强制下架商品','BUTTON',0);
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`) VALUES (7,'user:status','启用禁用用户','BUTTON',0);
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('ADMIN',1);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('ADMIN',2);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('ADMIN',3);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('ADMIN',4);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('ADMIN',5);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('ADMIN',6);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('BUYER',1);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('BUYER',2);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SELLER',1);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SELLER',2);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SUPER_ADMIN',1);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SUPER_ADMIN',2);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SUPER_ADMIN',3);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SUPER_ADMIN',4);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SUPER_ADMIN',5);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SUPER_ADMIN',6);
INSERT INTO `sys_role_permission` (`role_code`, `permission_id`) VALUES ('SUPER_ADMIN',7);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` (`id`, `seller_id`, `name`, `category`, `price`, `description`, `status`, `views`, `create_time`) VALUES ('P001','U005','罗技无线鼠标','电子',89.00,'静音版，99新，换新鼠标了所以出掉。','ON_SALE',0,'2026-11-17 08:17:22');
INSERT INTO `product` (`id`, `seller_id`, `name`, `category`, `price`, `description`, `status`, `views`, `create_time`) VALUES ('P002','U005','机械键盘 青轴','电子',200.00,'手感极佳，吃灰很久了。','ON_SALE',0,'2026-11-17 08:17:22');
INSERT INTO `product` (`id`, `seller_id`, `name`, `category`, `price`, `description`, `status`, `views`, `create_time`) VALUES ('P003','U006','考研英语真题卷','教材',15.00,'全新未写，买错版本了。','ON_SALE',0,'2026-11-17 08:17:22');
INSERT INTO `product` (`id`, `seller_id`, `name`, `category`, `price`, `description`, `status`, `views`, `create_time`) VALUES ('P004','U006','Java核心技术 卷1','教材',50.00,'书角有轻微磨损，内页无笔记。','PENDING',0,'2026-11-17 08:17:22');
INSERT INTO `product` (`id`, `seller_id`, `name`, `category`, `price`, `description`, `status`, `views`, `create_time`) VALUES ('P005','U005','阿迪达斯篮球','运动',120.00,'打了两次，手感很好。','SOLD',0,'2026-11-17 08:17:22');
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

