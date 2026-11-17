# exp3-web · 校园二手商品交易管理系统

《Java 企业级应用》实验 3：不使用 Spring / MyBatis，基于原生 Servlet + JSP + JDBC + MySQL 实现的前后端分离单体应用。前端在 JSP 中通过 CDN 引入 Vue 3 + Element Plus，后端通过自定义 `BaseServlet` 反射分发 `action` 请求。

## 功能概览

- 买家：浏览/搜索/分类/排序/分页、下单、确认收货、评价、查看评价
- 卖家：发布商品（待审核）、查看我的发布、确认发货、查看收到评价
- 管理员：商品审核（通过/驳回）、用户管理（关键字 + 角色筛选、封禁/解封）、违规评价软删除、审计日志
- 通用：登录/注册、RBAC 菜单权限、Session 登录拦截、操作审计日志

## 技术栈

| 层次 | 技术 |
| --- | --- |
| Web | Jakarta Servlet 6.0、JSP 3.1、Filter |
| 视图 | Vue 3.4、Element Plus 2.8、Axios（CDN） |
| 业务 | 原生 Java 21、反射路由、Service 层状态机 |
| 持久化 | 原生 JDBC、MySQL 8.0、手写事务 |
| 日志 | SLF4J + Logback（按天/大小滚动） |

## 目录结构

```
exp3-web
├── pom.xml
├── sql/exp3_trade_rbac.sql          # 建库、建表、演示数据
├── docs/CODE_REVIEW.md              # 代码审查与修复报告
└── src/main
    ├── java/com/zhj
    │   ├── audit/                   # 审计日志
    │   ├── dao/                     # JDBC 数据访问
    │   ├── filter/                  # 登录 + 角色拦截
    │   ├── infrastructure/          # DBUtil / JsonUtil / MD5 / IdUtil
    │   ├── model/                   # 实体
    │   ├── service/                 # 业务规则、状态机
    │   └── servlet/                 # BaseServlet + 各模块 Servlet
    ├── resources/logback.xml
    └── webapp
        ├── login.jsp / register.jsp / main.jsp
        ├── static/js/common.js      # Axios 封装
        └── views/*.jsp              # iframe 子页面
```

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < sql/exp3_trade_rbac.sql
```

> 脚本包含 `DROP TABLE`，会重建 `exp3_trade_rbac` 中的表，请勿在生产库执行。

默认库名 `exp3_trade_rbac`，字符集 `utf8mb4`。如果你的账号密码不是 `root/123456`，可通过环境变量覆盖，无需改代码：

```bash
# Windows PowerShell
$env:EXP3_DB_USER="root"
$env:EXP3_DB_PASSWORD="你的密码"
# 也可以使用 JVM 参数：-Dexp3.db.url=... -Dexp3.db.user=... -Dexp3.db.password=...
```

### 2. 打包

```bash
mvn clean package
```

产物：`target/exp3-web-1.0-SNAPSHOT.war`。

### 3. 部署到 Tomcat 10.1

前端 `static/js/common.js` 中的 `baseURL` 固定为 `/exp3/api`，因此 **WAR 必须部署为 `exp3.war`**（或把 IDEA 的 Application context 设为 `/exp3`）：

```bash
copy target\exp3-web-1.0-SNAPSHOT.war %CATALINA_HOME%\webapps\exp3.war
%CATALINA_HOME%\bin\startup.bat
```

访问 <http://localhost:8080/exp3/>。

IDEA 用户：Run/Debug Configurations → Tomcat Server → Deployment，把 war 的 Application context 设为 `/exp3` 即可。

### 4. 演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | 超级管理员 |
| manager | 123456 | 管理员 |
| buyer1 / buyer2 | 123456 | 买家（余额 1000 / 500） |
| seller1 / seller2 | 123456 | 卖家 |

## 接口约定

- 所有接口位于 `/api/{module}?action={method}`，由 `BaseServlet` 反射调用同名 public 方法。
- 统一响应：`{ "code": 200, "message": "success", "data": ... }`；业务错误使用 `400/401/403/404`，HTTP 状态码仍为 200，由前端 Axios 拦截器统一提示。
- 写操作只接受 `POST`；只读动作（list/myList/pendingList/getByOrder/readLog）支持 `GET`。

## 安全说明

- 密码使用 MD5 存储（课程沿用），**生产环境应改为 BCrypt/PBKDF2 加盐哈希**。
- 所有 SQL 均使用 `PreparedStatement`；动态排序字段走白名单，不存在拼接注入。
- 已修复：注册提权、订单状态伪造、越权确认/收货、超卖、评价伪造、密码散列泄露、审计日志越权、封禁会话失效等，详见 [docs/CODE_REVIEW.md](docs/CODE_REVIEW.md)。
- 仍建议后续补充：CSRF Token、登录失败限流、数据库连接池（HikariCP）、HTTPS + Secure Cookie。

## 常见问题

1. **页面能打开但请求全部 401**：先确认部署上下文是 `/exp3`，再确认浏览器已登录。
2. **前端报 `roleCode=undefined` 或筛选无效**：不要在 JSP 的 `<script>` 中使用 ES 模板字符串的 `${...}`，JSP 会把它当 EL 在服务端求值。统一用 Axios `params` 传参。
3. **日志页 404**：审计日志写在 `${catalina.base}/logs/audit.log`，先执行一次下单/审核等操作再刷新。
4. **中文乱码**：确认数据库为 `utf8mb4`，Tomcat 启动参数包含 `-Dfile.encoding=UTF-8`。
