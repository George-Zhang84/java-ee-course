# 实验3 代码审查与修复报告

> 审查对象：`exp3-web`（校园二手商品交易管理系统）
> 审查方式：静态代码审查 + 本地 Tomcat 10.1.61 / MySQL 8.0 实际运行 + Headless Chrome 页面验证 + 50 项接口回归 + 8 并发抢购测试
> 审查时间：2026-11-17

## 0. 结论摘要

原项目功能完整、分层清晰，反射路由和事务控制的思路是对的，但存在 **1 个直接导致功能失效的前端致命 Bug、4 个可被直接利用的越权/资金漏洞、若干健壮性问题**。本次共修复 18 类问题，全部通过回归验证。

其中最典型的、也是你反馈的「用户管理按角色筛选不生效」，根因并不在 SQL 或后端，而是：

**JSP 会把 `<script>` 里 JS 模板字符串的 `${...}` 当成 EL 表达式，在服务端提前求值成空字符串。**

`admin-user.jsp` 源码：

```js
api.get(`/user?action=list&keyword=${searchKeyword.value}&roleCode=${searchRole.value}`)
```

浏览器实际收到的 JS：

```js
api.get(`/user?action=list&keyword=&roleCode=`)
```

所以无论选哪个角色，发给后端的永远是空 `roleCode`。同样的问题也让商品关键字搜索、`order-list.jsp` 的「查看评价」一起失效。

---

## 1. 致命问题（已修复）

### P0-1 JSP EL 吃掉前端模板字符串，角色筛选/搜索/查看评价失效

- **位置**：`views/admin-user.jsp`、`views/order-list.jsp`
- **现象**：用户管理选「卖家」仍显示全部用户；商品搜索无效果；订单页点「查看评价」提示找不到记录。
- **根因**：JSP 在服务端先解析 `${...}`（EL），而 Vue 的 `[[ ]]` 分隔符只解决插值，不解决 `<script>` 内的字符串。`${searchRole.value}` 被求值为空串。
- **修复**：
  - 全部改为 Axios `params` 传参，不再手拼 URL：`api.get('/user', { params: { action: 'list', keyword, roleCode } })`；
  - `order-list.jsp` 改为 `api.get('/review', { params: { action: 'getByOrder', orderId } })`；
  - 全仓库 JSP 已无 `${...}`（含 JS 注释）。
- **验证**：Headless Chrome 点击「卖家」后请求变为 `roleCode=SELLER`，表格只剩 seller1/seller2；SQL 复核 `role_code='SELLER'` 返回 2 行。

### P0-2 注册接口可伪造角色，直接注册成管理员（垂直提权）

- **位置**：`servlet/auth/LoginServlet.register`、`service/AuthService.register`
- **问题**：`roleCode` 完全信任请求体，`POST /api/auth?action=register` 传 `"roleCode":"SUPER_ADMIN"` 即可获得管理员权限。
- **修复**：服务端白名单只允许 `BUYER`/`SELLER`，并校验用户名 3-20 位、密码 6-32 位、店铺名 ≤100；非法角色直接 400。
- **验证**：注册 `ADMIN`、弱密码均被 400 拒绝；正常买家注册成功。

### P0-3 订单状态机信任前端 `currentStatus`，可伪造状态、越权操作

- **位置**：`service/OrderService.updateOrderStatus`、`servlet/order/OrderServlet`
- **问题**：
  - `currentStatus` 由前端传入并直接作为校验依据，买家可伪造 `currentStatus=CONFIRMED` 把 PENDING 订单改成 RECEIVED；
  - 只校验角色、不校验订单归属，任意卖家可确认别人的订单；
  - `newStatus` 除 CONFIRMED/RECEIVED 外不校验，可写入任意状态。
- **修复**：服务端重新读取订单，按数据库真实 `status` + `buyer_id/seller_id` 校验；只允许 `PENDING→CONFIRMED`、`CONFIRMED→RECEIVED`；SQL 更新带 `status` 与归属条件（CAS），重复提交/并发操作返回「订单状态已变更」。
- **验证**：伪造状态、跨卖家确认、跨买家收货、非法状态全部 400/拒绝。

### P0-4 下单「先查后改」导致超卖与余额扣成负数

- **位置**：`dao/OrderDao.createOrder`、`service/OrderService`
- **问题**：商品是否在售、余额是否充足都在事务外基于 Session 旧数据判断，两个买家同时买最后一件会都成功；同一用户并发下单可把余额扣负。
- **修复**：
  - 商品抢占：`UPDATE product SET status='SOLD' WHERE id=? AND status='ON_SALE'`，影响行数必须为 1；
  - 余额扣减：`UPDATE sys_user SET balance=balance-? WHERE id=? AND balance>=?`，不再使用 Session 里的余额；
  - 三步同一事务，任一步失败整体回滚。
- **验证**：8 个并发请求抢购同一商品，**1 个成功、7 个返回「手慢了，商品已被其他买家抢购或已下架」**，订单表只有 1 条记录。

### P0-5 确认收货不给卖家结算，货款凭空消失

- **位置**：`dao/OrderDao.completeOrder`（新增）
- **问题**：原实现只把订单状态改为 `RECEIVED`，卖家的 `balance` 永远不增加。
- **修复**：`FOR UPDATE` 锁定订单 → 校验买家与状态 → 改状态 → `balance = balance + amount` 给卖家入账，同一事务提交；重复收货因状态条件不满足而拒绝，不会重复打款。
- **验证**：89 元订单收货后 seller1 余额从 0 变 89；重复收货 400 且余额不变。

---

## 2. 高危问题（已修复）

### P1-1 评价可伪造：任意订单、任意卖家、任意评分

- **位置**：`service/ReviewService.submitReview`、`servlet/review/ReviewServlet`
- **问题**：`sellerId` 由前端传入，且不校验订单是否属于当前买家、是否已收货；可给任意订单刷评价，甚至给别的卖家造差评。
- **修复**：服务端按 `orderId` 查订单，校验买家归属、订单状态必须为 `RECEIVED`、评分 1-5、内容非空且 ≤255；`sellerId` 一律取订单中的卖家，忽略前端传值；同一订单只允许一条有效评价。
- **验证**：评价他人订单/评分 9/空内容均 400；伪造 `sellerId=U006` 后数据库仍写入真实卖家 `U005`；同订单重复评价被拒；管理员软删除后可重新评价（闭环）。

### P1-2 用户列表接口把密码散列返回给前端

- **位置**：`dao/UserDao.findAll`
- **问题**：`SELECT *` + 实体直接序列化，响应里带 `password`（MD5），管理员浏览器可拿到全站密码散列。
- **修复**：列表映射后统一 `setPassword(null)`（Fastjson2 默认不序列化 null 字段），登录响应同样置空。
- **验证**：`/api/user?action=list` 响应已无 `password` 字段。

### P1-3 审计日志接口没有权限校验

- **位置**：`servlet/audit/AuditServlet.readLog`
- **问题**：任意登录用户都能读取全站操作日志（含用户名、商品、订单信息）。
- **修复**：仅 `ADMIN`/`SUPER_ADMIN`；同时把 `catalina.base` 为空时的路径拼接改为 `File` 方式，避免 `null/logs/...`。
- **验证**：买家 403，管理员 200。

### P1-4 封禁用户后，旧 Session 仍可继续操作

- **位置**：`filter/AuthFilter`
- **问题**：只校验 Session 中是否存在用户，管理员封禁后已登录会话仍然有效。
- **修复**：每次 API 请求按 `id` 重新加载用户，校验 `status==1`，并刷新 Session 中的余额/角色；被禁用则 `invalidate()` 并返回 401。
- **验证**：管理员封禁 buyer2 后，buyer2 的旧会话下一次请求返回 401。

### P1-5 商品审核状态不校验，且可重复审核

- **位置**：`servlet/product/ProductServlet.audit`、`dao/ProductDao.audit`
- **问题**：`status` 任意值都能写入（甚至改成 SOLD/PENDING），已审核商品还能再改。
- **修复**：白名单 `ON_SALE`/`OFFLINE`；SQL 带 `status='PENDING'` 条件，重复审核返回 400。
- **验证**：`status=HACK` 400，重复审核 400。

### P1-6 反射调用异常被吞、错误信息可能泄露内部细节

- **位置**：`servlet/BaseServlet`
- **问题**：`method.invoke` 的 `InvocationTargetException` 被统一捕获，真实异常没有日志；原实现把 `e.getMessage()` 直接返回前端，可能泄露 SQL/表名。
- **修复**：解包 `InvocationTargetException`，用 SLF4J 记录 cause；前端只返回「服务器内部错误」；action 增加 `[A-Za-z][A-Za-z0-9]*` 白名单；JSON 解析失败映射为 400。
- **验证**：非法 action 400、不存在 action 404、非法 JSON 400 而非 500。

---

## 3. 中低危 / 健壮性问题（已修复）

| 编号 | 位置 | 问题 | 修复 |
| --- | --- | --- | --- |
| P2-1 | 各 Service | `"O"+System.currentTimeMillis()` 生成主键，同毫秒并发会主键冲突 | 新增 `IdUtil`，UUID 截断到 32 位 |
| P2-2 | `static/js/common.js` | 401 时跳转相对路径 `login.jsp`，在 iframe 中会跳到 `/exp3/views/login.jsp` 404 | 统一跳 `/exp3/login.jsp`，使用 `window.top`，并加单次跳转标记防重复提示 |
| P2-3 | `servlet/product/ProductServlet.list` | `page/pageSize` 无边界，负数 OFFSET 或超大 pageSize 可能导致 SQL 异常/压力 | page≥1，pageSize 限制 1-50 |
| P2-4 | `servlet/user/UserServlet.updateStatus` | `status` 可为 null/任意整数，null 会 NPE；普通管理员可操作管理员账号 | 只允许 0/1；普通管理员只能管理买家/卖家；禁止操作自己 |
| P2-5 | `servlet/product/ProductServlet.create` | 价格可为 null/负数、分类任意、名称超长触发 SQL 异常 | 名称 1-100、分类白名单、价格 (0, 999999.99]、描述 ≤1000 |
| P2-6 | `filter/AuthFilter` | 反射 `Origin` 并允许 credentials，等于允许任意站点跨域携带 Cookie | 删除 CORS 响应头（同源部署不需要） |
| P2-7 | `servlet/auth/LoginServlet.login` | 登录未更换 Session ID，存在会话固定风险 | 登录前销毁旧 Session，再创建新 Session |
| P2-8 | `resources/logback.xml` | `audit.log` 无限增长；`catalina.base` 未定义时路径异常 | 按天 + 10MB 滚动，保留 7 天，总大小 100MB；`${catalina.base:-.}` 兜底 |
| P2-9 | `audit/AuditLogger` | 商品名/评价内容中的换行可伪造日志行（日志注入） | 统一替换 `\r`/`\n` |
| P2-10 | `web.xml` | 未配置会话超时与 Cookie HttpOnly | 30 分钟超时、HttpOnly、Cookie tracking |
| P2-11 | `BaseServlet` | 写操作也能用 GET 触发，扩大 CSRF 攻击面 | 写动作只允许 POST（只读动作白名单），非法方法 405 |
| P2-12 | `common.js` | 收到非 JSON/HTTP 错误时统一提示「网络错误」，丢失后端 message | 优先使用响应体 message，401 单独处理 |
| P2-13 | `main.jsp` | `JSON.parse(null)` 得到 null 导致模板报错；图标 CDN 未锁版本 | 空值兜底并跳登录；图标固定 `@2.3.1` |
| P2-14 | `JsonUtil` | 非法 JSON 抛 unchecked 异常变 500；请求体无上限 | 包装为 `IllegalArgumentException`→400，限制 1MB |

---

## 4. 确认不存在 / 已规避的问题

- **SQL 注入**：所有查询均为 `PreparedStatement` 参数绑定；排序字段是固定字符串分支（`price_asc/price_desc`），`LIMIT/OFFSET` 也走占位符，未发现拼接注入。
- **存储型 XSS**：页面全部使用 Vue 文本插值 `[[ ]]`，未使用 `v-html`；商品名、评价内容会被自动转义。
- **N+1 查询**：订单列表用 `JOIN` + `EXISTS` 一次取完商品/买卖家/是否评价；商品列表 `JOIN` 卖家；评价列表 `JOIN` 订单/商品/买家，未发现循环查库。
- **连接泄漏**：DAO 全部使用 try-with-resources；事务方法在 `finally` 中关闭连接，异常路径会回滚。

---

## 5. 仍存在的限制（课程范围内未改，建议后续处理）

1. **MD5 无盐**：`sys_user.password` 是 32 位 MD5，彩虹表可反查。改造需要加盐列或改为 PBKDF2/BCrypt，且要迁移现有账号，本次保持兼容未动。
2. **CSRF**：已通过「写操作只允许 POST」降低风险，但未引入 CSRF Token；如需彻底防护，可在登录时下发 token，Axios 统一带 `X-CSRF-Token`。
3. **无连接池**：`DBUtil` 每次 `DriverManager.getConnection()`，高并发下性能有限；可引入 HikariCP（不违反“禁用 Spring/MyBatis”约束）。
4. **重复评价并发窗口**：`existsByOrderId` 与 `insert` 之间仍有极小竞态，表上没有唯一约束；可增加生成列唯一索引（`status=1` 时唯一）彻底解决。
5. **登录无限流/验证码**：可被暴力破解，建议按 IP/账号限流。
6. **前端依赖 CDN**：离线环境无法加载 Vue/Element Plus，答辩前建议把静态资源下载到 `webapp/static` 本地引用。
7. **Cookie 未设置 Secure**：本地 HTTP 演示无法设置，上线 HTTPS 后应开启。
8. **金额使用 `double`**：当前金额计算用 `double` + 数据库 `decimal(10,2)`，课程金额范围内没问题；正式系统建议改用 `BigDecimal` 并在数据库层统一舍入规则。
9. **`product.views` 字段未使用**：可以补一个「查看详情时浏览量 +1」的功能，不属于本次 Bug 修复范围。

---

## 6. 验证记录

### 6.1 接口回归（50/50 通过）

覆盖：登录/注册、角色筛选、关键字+角色组合、密码脱敏、下单扣款、重复购买、卖家/买家越权、状态机、重复收货、卖家结算、评价归属/评分/重复评价/软删除后重评、商品发布校验、审核白名单、用户封禁与旧会话失效、审计日志权限、非法 action/JSON/未登录。

```text
===== 结果: PASS=50 FAIL=0 =====
```

### 6.2 并发抢购

8 个并发请求（buyer1/buyer2 各 4 个）抢购同一件商品：

```text
{ '200 | success': 1,
  '400 | 手慢了，商品已被其他买家抢购或已下架': 7 }
success count = 1
```

### 6.3 浏览器页面验证（Headless Chrome）

- `login.jsp`、`main.jsp`、`admin-user.jsp`、`admin-audit.jsp`、`admin-reviews.jsp`、`audit-log.jsp`、`product-list.jsp`、`order-list.jsp`、`my-products.jsp`、`seller-reviews.jsp` 均无 JS 异常/警告；
- 用户管理选择「卖家」后，表格由 7 行变为 seller1/seller2 两行，请求 URL 为 `roleCode=SELLER`；
- 清除 Cookie 后调用接口，顶层窗口正确跳转 `/exp3/login.jsp`（不再 404）。

### 6.4 数据安全

测试前已备份 `exp3_trade_rbac`，两轮测试后均从容器内备份完整还原：用户 6、商品 5、订单 2、评价 0，余额与初始状态一致。

---

## 7. 主要变更文件

**后端**

- `servlet/BaseServlet.java`：action 校验、异常解包、写操作 POST 限制
- `filter/AuthFilter.java`：状态复核、角色白名单、移除 CORS 反射
- `service/AuthService.java`、`servlet/auth/LoginServlet.java`：注册白名单、会话固定修复
- `dao/OrderDao.java`、`service/OrderService.java`、`servlet/order/OrderServlet.java`：事务抢占、状态机、卖家结算
- `dao/UserDao.java`、`servlet/user/UserServlet.java`：密码脱敏、状态/层级校验
- `dao/ProductDao.java`、`servlet/product/ProductServlet.java`：审核 CAS、输入校验
- `dao/ReviewDao.java`、`service/ReviewService.java`、`servlet/review/ReviewServlet.java`：评价归属与参数校验
- `servlet/audit/AuditServlet.java`、`audit/AuditLogger.java`：权限、路径、日志注入
- `infrastructure/DBUtil.java`、`JsonUtil.java`、`IdUtil.java`：配置化、JSON 错误、主键生成
- `web.xml`、`logback.xml`：会话安全、日志滚动

**前端**

- `static/js/common.js`：401 跳转、错误提示
- `views/admin-user.jsp`：角色筛选（核心 Bug）
- `views/order-list.jsp`：评价查看/提交、状态更新
- `views/product-list.jsp`、`seller-reviews.jsp`、`admin-*.jsp`、`audit-log.jsp`、`my-products.jsp`：统一 params、空值兜底
- `login.jsp`、`register.jsp`、`main.jsp`：校验、缓存清理、图标版本锁定
