# Java 企业级应用课程实验

本仓库包含《Java企业级应用》课程实验 1~4 的完整代码，四个实验层层递进：

| 实验 | 目录 | 内容 | 技术栈 |
|---|---|---|---|
| 实验一 | `exp1-trade` | 基于命令行的校园二手交易系统 | Java SE + 内存集合 |
| 实验二 | `exp2-rbac` | 基于命令行的权限管理系统 | JDBC + 注解 + 反射 |
| 实验三 | `exp3-web` | 校园二手商品交易管理系统（单体 Web） | Servlet + JSP + Vue3(CDN) + JDBC + MySQL |
| 实验四 | `exp4-springboot` | 前后端分离重构（本仓库重点） | Spring Boot 3.3.5 + Spring Security 6 + MyBatis-Plus + MySQL 8 + Vue3/Vite/Element Plus/ECharts |

## 实验四快速入口

- 说明与启动：[exp4-springboot/README.md](exp4-springboot/README.md)
- 设计文档：[总体设计](exp4-springboot/docs/01-总体设计.md) · [数据与接口](exp4-springboot/docs/02-数据与接口.md) · [测试报告](exp4-springboot/docs/03-测试报告.md) · [部署手册](exp4-springboot/docs/04-部署与运行手册.md) · [代码自查](exp4-springboot/docs/CODE_REVIEW.md)
- 数据库脚本：[sql/exp4_trade.sql](exp4-springboot/sql/exp4_trade.sql)（16 张表 + 演示数据）

```powershell
$env:JAVA_HOME = 'F:\Java\ms-21.0.12.1'
cd exp4-springboot\backend; mvn spring-boot:run    # 后端 8080
cd ..\frontend; npm run dev                        # 前端 5173
```

演示账号（密码均 `123456`）：`admin`(超管)、`manager`(管理员)、`buyer1`、`buyer2`、`seller1`、`seller2`。
