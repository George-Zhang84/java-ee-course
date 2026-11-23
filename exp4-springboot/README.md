# 实验四：校园二手商品交易管理系统（SpringBoot + Vue3 + MySQL）

实验一~三的成果在实验四中升级为 **前后端分离** 的企业级应用：后端 Spring Boot 3.3.5 + Spring Security 6 + MyBatis-Plus + MySQL 8，前端 Vue 3 + Vite + Element Plus + ECharts。

## 目录结构

```
exp4-springboot/
├── backend/                 # Spring Boot 后端（142 个 Java 文件）
│   ├── src/main/java/com/zhj/exp4/
│   │   ├── common/          # 统一响应、异常、分页、traceId
│   │   ├── config/          # 安全、MyBatis-Plus、Jackson、OpenAPI、异步
│   │   ├── security/        # JWT、过滤器、权限缓存、黑名单
│   │   ├── controller/      # 认证/商品/订单/议价/钱包/评价/通知/管理端
│   │   ├── service/         # 业务接口与实现（事务边界）
│   │   ├── mapper/          # MyBatis-Plus Mapper + CAS/统计 SQL
│   │   ├── entity/ enums/   # 16 张表实体 + 状态枚举
│   │   ├── aspect/          # @Audit 审计切面
│   │   ├── event/ listener/ # 订单支付领域事件
│   │   └── task/            # 订单超时、报价过期定时任务
│   └── src/test/java/       # 28 个 JUnit 用例
├── frontend/                # Vue3 SPA（13 个业务页面）
├── sql/exp4_trade.sql       # 建库建表 + 演示数据（16 张表）
└── docs/
    ├── 01-总体设计.md        # 功能/创新点/设计模式/架构/流程图
    ├── 02-数据与接口.md      # ER 图、数据字典、45 个接口、安全设计
    ├── 03-测试报告.md        # 单元/接口/页面三层测试结果
    ├── 04-部署与运行手册.md   # 环境、启动、打包、配置、账号
    ├── CODE_REVIEW.md        # 自查发现的问题与修复
    └── shots/                # 页面截图（报告用）
```

## 快速开始

```powershell
$env:JAVA_HOME = 'F:\Java\ms-21.0.12.1'
cmd /c "docker exec -i mysql-rbac mysql -uroot -p123456 --default-character-set=utf8mb4 < .\sql\exp4_trade.sql"
cd backend; mvn spring-boot:run          # 8080
cd ..\frontend; npm install; npm run dev # 5173
```

演示账号：`admin / manager / buyer1 / buyer2 / seller1 / seller2`，密码均为 `123456`。

## 主要特色

1. **结构化议价**：出价-接受/拒绝/撤回-24h 过期；**达到卖家底价自动成交**，接受/自动成交共用同一套 CAS 锁商品流程，防一物两卖。
2. **订单超时自动取消**：`@Scheduled` 扫描 + 独立事务逐笔回滚商品与报价；时间统一 Asia/Shanghai，避免"刚下单就被取消"。
3. **数据看板**：聚合 SQL + ECharts（趋势、分类占比、热销 Top5），零值分类自动过滤。
4. **AOP 审计**：`@Audit` 注解 + 异步落库，记录操作人/IP/入参摘要/耗时/结果；登录事件独立记入 `login_log`。
5. **多图上传治理**：白名单 + MIME 校验 + 随机文件名 + 路径归属校验 + **孤儿文件定时清理**。
6. **事件驱动**：支付成功发布 `OrderPaidEvent`，`AFTER_COMMIT` 异步通知。
7. **浏览量防刷**：同一用户/IP 30 分钟只计一次，卖家自看不计数。
8. **权限真实生效**：菜单、按钮、接口、数据四级都按 RBAC 权限码校验，管理员看不到个人钱包/订单菜单。

## 角色与权限（31 个权限点，83 条角色权限）

| 角色 | 权限数 | 说明 |
|---|---|---|
| SUPER_ADMIN | 31 | 全部权限，可改角色权限矩阵 |
| ADMIN | 12 | 审核商品、管用户、看板、审计（不含个人买卖功能） |
| USER | 20 | **人人可买可卖**：浏览/下单/发布/议价/钱包/评价 |
| SELLER | 20 | 保留角色，仅作"认证卖家"标识，功能与 USER 一致 |

## 技术亮点（相对实验三）

| 维度 | 实验三 | 实验四 |
|---|---|---|
| 分层 | Servlet + JDBC | Controller → Service → Mapper，DTO/PO 分离 |
| 认证 | Session + MD5 | JWT 无状态 + BCrypt + 令牌黑名单 |
| 授权 | 单角色字段 | 5 表 RBAC 多角色 + `@PreAuthorize` + 数据级校验 |
| 并发 | 先查后改 | 商品/订单/报价三处 CAS + 余额条件更新 |
| 可观测 | 手写日志 | traceId + AOP 审计 + Actuator |
| 前端 | JSP 内 CDN | Vue3 + Vite SPA + 路由守卫 + 权限菜单 |
