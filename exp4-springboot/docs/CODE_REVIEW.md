# 实验四 代码自查与缺陷修复记录（CODE_REVIEW）

> 本文件记录开发过程中的自我审查：发现的问题、根因、修复方式与验证方法。
> 既是实验报告"调试过程"的素材，也便于后续维护者理解关键决策。

---

## 1 后端问题

### 1.1 `PaginationInnerInterceptor` 找不到（编译失败）

- **现象**：`cannot find symbol: class PaginationInnerInterceptor`。
- **根因**：MyBatis-Plus 从 **3.5.9** 起把 jsqlparser 依赖拆成独立模块，分页插件不再随 `mybatis-plus-spring-boot3-starter` 传递。
- **修复**：`pom.xml` 显式引入 `com.baomidou:mybatis-plus-jsqlparser:3.5.9`。
- **验证**：`mvn compile` 通过，分页接口返回正确 `total`。

### 1.2 审计注解与实体类同名冲突

- **现象**：切面方法参数类型解析异常，审计不生效。
- **根因**：`aspect.AuditLog`（注解）与 `entity.AuditLog`（实体）同名，切面里 `AuditLog auditLogAnnotation` 实际指向实体类。
- **修复**：注解改名为 `@Audit`，实体保持 `AuditLog`，语义更清晰。
- **验证**：AOP 审计日志正常落库（管理端审计页可见）。

### 1.3 Spring Boot 生成随机安全密码告警

- **现象**：启动日志出现 `Using generated security password`，并注入 `InMemoryUserDetailsManager`。
- **根因**：引入 `spring-boot-starter-security` 后自动配置会创建默认用户。
- **修复**：`application.yml` 排除 `UserDetailsServiceAutoConfiguration`（本项目用 JWT 自建认证）。
- **验证**：启动日志无告警，认证流程不受影响。

### 1.4 `order` 是 MySQL 保留字

- **现象**：若不处理，MyBatis-Plus 生成的 `SELECT ... FROM order` 会语法错误。
- **修复**：实体 `@TableName("`order`")` 带反引号；所有手写 SQL 同样加反引号。
- **验证**：订单增删查改与统计 SQL 全部正常。

### 1.5 金额与状态一致性

- **自查发现**：早期"先查余额再扣款"的写法在并发下会超扣。
- **修复**：改为 `UPDATE sys_user SET balance = balance - #{amount} WHERE id=? AND balance >= #{amount}`；影响行数为 0 即余额不足（`SysUserMapper.deductBalance`）。
- **同类修复**：商品锁库存、订单支付、报价接受全部改为带 `WHERE status = ?` 的 CAS 更新。
- **验证**：`WalletServiceImplTest` 断言余额不足时不写流水；接口测试验证重复支付返回 2002。

### 1.6 定时任务与事务边界

- **自查发现**：若把"扫描 + 逐笔取消"整体放在一个 `@Transactional` 方法里，会形成长事务且单笔失败拖垮整批；类内 `this.cancel()` 调用还不走代理，事务失效。
- **修复**：任务类只做遍历，逐条调用 `OrderService.cancelBySystem(id)`（跨 Bean 调用，独立事务）。
- **验证**：超时订单被逐笔取消，日志打印"待处理 N 笔 / 成功 M 笔"。

### 1.7 中文编码

- **现象**：用 `mysql -e` 查询时控制台显示 `????`。
- **根因**：Windows 控制台代码页为 GBK，与数据库无关。
- **验证方式**：改用 `WHERE title LIKE '%键盘%'` 能命中且 `COUNT=1`，证明库内 UTF-8 正确；Docker 导入时显式 `--default-character-set=utf8mb4`。

---

## 2 前端问题

### 2.1 Vite 开发服务器因 EBUSY 崩溃

- **现象**：修改文件后 dev server 直接退出：`Error: EBUSY ... .Users.vue.xxxx.tmpdir/Users.vue.tmp`。
- **根因**：编辑器类工具采用"临时目录 + 原子改名"保存文件，Vite 的 `fs.watch` 尝试监听临时文件时失败。
- **修复**：`server.watch.ignored` 忽略 `**/.*.tmpdir/**`、`**/*.tmp`。
- **验证**：连续修改多个文件后 dev server 稳定，HMR 正常。

### 2.2 顶部导航溢出

- **现象**：1440 宽度下 10 个菜单项把"后台管理"挤出可视区。
- **修复**：顶栏只保留高频入口（首页/商品市场/发布/我的商品/议价/订单），收藏、钱包、消息、个人中心收进头像下拉；菜单改为 `computed`，登录态变化即时刷新。
- **验证**：截图 `home3.png`（未登录只有 2 个入口）、`admin_dash2.png`（出现"后台管理"下拉）。

### 2.3 分区标题被挤到右侧

- **现象**："按分类逛逛""最新上架"等标题跑到右边，左边只剩一条蓝色竖线。
- **根因**：`.section-title` 用了 `justify-content: space-between`，左侧色条是 `::before` 伪元素——伪元素也是 flex item，于是文本被推到另一端。
- **修复**：改 `flex-start`，右侧"查看全部"用 `.more { margin-left: auto }` 单独控制。

### 2.4 订单页表格行不渲染

- **现象**：接口返回 `total=2`、页面内调试行确认 `list.length=2`，但 `el-table` 一行都没画出来，表头正常。
- **排查**：先在模板里插入临时调试行输出 `list.length / list[0].orderNo`，确认是渲染层而非数据层问题；再排除 `fixed` 列、`el-tabs` 容器等因素（同类表格在"我的商品""审计日志"页正常）。
- **修复**：该页改为卡片式列表（保留全部筛选与操作），既绕开 Element Plus 的布局边界问题，也更适合订单这种信息量大的条目；调试代码已删除。
- **验证**：截图 `orders5.png` 正常展示两条订单与"已评价"状态。

### 2.5 示例图片 404 导致裂图

- **修复**：所有 `<img>` 增加 `@error` 回退占位图；同时用 PIL 生成 6 张演示图到 `uploads/demo/`（已被 `.gitignore` 忽略，不入库）。

### 2.6 截图辅助文件的安全隐患

- **问题**：为自动化截图临时创建过 `frontend/public/_devlogin.html`（把 URL 上的 token 写入 localStorage 后跳转），若随生产构建发布，等于开放一个"免登录"入口。
- **处理**：截图完成后立即删除，并在**生产构建前**再次确认目录不存在该文件。
- **教训**：任何写 localStorage / 跳过登录的调试代码都必须与生产构建隔离。

---

## 4 用户试用反馈修复（第二轮）

第一版交付后按真实操作路径试用，发现 8 个问题，均已修复并回归验证。

| # | 反馈现象 | 根因 | 修复 |
|---|---|---|---|
| 1 | buyer1 登录看不到"发布商品"，新注册账号点"我要卖东西"提示无权限 | 权限矩阵把 `product:publish/edit/offline`、`bargain:handle`、`order:ship` 只给了 SELLER 角色；注册只发 USER | **校园场景人人可买可卖**：把这些权限合并进 USER（18 项，与 SELLER 一致）；SELLER 降级为"认证卖家"标识。注册说明文案同步改为"注册后即可买也可卖" |
| 2 | 通知角标显示 2，点进去一直转圈无内容 | `Notifications.vue` 模板用了 `fromNow()` 但**忘记 import**，渲染期抛错，`v-loading` 蒙层卡住 | 补上 `import { fromNow } from '@/utils/format'` |
| 3 | 登录页左侧品牌区文字挤在左上角 | 容器没有垂直布局约束 | `.auth-intro` 改为 flex 纵向居中 + `text-align:center`，说明文字限宽 |
| 4 | 分类缺少"其他" | 初始数据只有 5 个分类 | 新增 `(6,'其他','MoreFilled',6,1)` |
| 5 | 后台审核列表点商品 →"商品不存在或已下架" | 详情接口只对"卖家本人"放行非公开状态，管理员也被拦 | `detail()` 增加管理员可见分支；同时把 `auditRemark`（审核意见）回给卖家与管理员，买家仍为 null |
| 6 | 信用分会超过 100，不清楚怎么算 | 规则未在任何界面说明，且上限设为 200 | 上限收紧为 **0~150**（初始 100，好评 +1、差评 -2），商品详情与个人中心增加说明与 tooltip |
| 7 | 看板趋势图"成交额"轴名与图例重叠、饼图标签压住图例 | ECharts 布局参数不当 | 图例移到底部、`grid.top` 加到 56；饼图改为"图例竖排右侧 + 扇区内百分比"，并过滤 0 值分类 |
| 8 | 后台管理入口在 1440 宽下被顶栏挤出 | 顶栏菜单项过多 | 顶栏只保留高频入口，其余收进头像下拉（上一轮已修，本轮回归确认） |

### 回归验证

- 接口层：buyer1 权限 18 项含 `product:publish`，可成功发布商品；新注册账号同样具备；管理员可打开待审核商品详情，普通买家仍返回 1004；分类 6 个（含"其他"）；通知列表正常返回。
- 页面层：重新截图确认登录页居中、通知页正常渲染、看板图表无重叠、后台可预览待审核商品。
- 单元测试：`mvn test` 28 项全绿。

---

## 6 第三轮：全流程联调修复

用户按真实路径试用后反馈 14 个问题，另做全流程回归（26 项断言全 PASS）。修复清单：

| # | 问题 | 根因 | 修复 |
|---|---|---|---|
| 1 | 管理员只有 12 权限却能看到钱包/订单 | 前端路由与下拉只判断登录态，`menu:*` 权限码未真正使用 | 路由 meta 全部补权限码；下拉按权限过滤；新增 `menu:favorites`/`menu:message` 并授予 USER/SELLER/**SUPER_ADMIN**（本轮补漏） |
| 2 | 审计表出现 `anonymous 认证/登录` | 登录接口同时写 `@Audit` 与 `login_log`，登录时无认证信息 | 登录去掉 `@Audit`，认证事件只归 `login_log` |
| 3 | 取消发布后图片残留 | 上传即落盘、无回收 | 新增删除图片接口（表单移除即删）+ 每 6 小时清理孤儿文件（未被引用且 >24h） |
| 4 | 分类筛选下拉显示数字 id | 字典未加载完就赋值 | 先加载字典再回填选中值 + 未加载时禁用 |
| 5 | 未读数不实时、0 仍显示红点 | 计数只在布局层 60s 轮询 | 抽出 `notify` store，读消息/全部已读/路由切换/20s 轮询统一刷新；`unread===0` 不渲染；`max=99` |
| 6 | 登录日志 13:12、操作日志 21:33 | MySQL 会话时区 UTC，`CURRENT_TIMESTAMP` 按 UTC 写 | JDBC 加 `connectionTimeZone=Asia/Shanghai&forceConnectionTimeZoneToSession=true`；关键业务时间由 Java 显式写入 |
| 7 | 详情页可刷浏览量 | 每次访问无条件 +1 | 同一用户/IP 30 分钟只计一次，卖家自看不计数 |
| 8 | 设了底价却不自动成交 | 底价只存不用 | 实现底价策略：出价 ≥ 底价自动成交（与人工接受共用 CAS 流程）；未设底价则全部人工确认 |
| 9 | 审核不填意见报"系统繁忙" | `updateById` 在字段全 null 时生成无 SET 的 SQL | 改 `lambdaUpdate().set(...)` 显式置空 |
| 10 | 议价弹窗两个数值含义不明 | 原价与预填出价未说明 | 明确标注"商品原价/我的出价"+ 规则说明 |
| 11 | 新议价到来未读数不涨 | 同 #5 | 同 #5 |
| 12 | 接受议价后订单立即被取消 | 订单 `create_time` 用 DB 默认值（UTC），被超时任务当作已超时 | 同 #6 + 订单时间显式写入；回归验证自动成交订单不再被误取消 |
| 13 | 卖家看不到自己的底价 | 详情对所有角色都不返回 `minPrice` | 卖家本人与管理员可见，其余为 null，并标注「仅你可见」 |
| 14 | 卖家评买家后无查看入口 | 只有"买家评卖家"展示 | 新增 `/api/reviews/received`，个人中心加「我收到的评价」 |

**潜在问题**：`NoResourceFoundException` 原被兜底处理成 `HTTP 200 + 5000`，导致"图片已删除仍返回 200"；已单独映射为 404。

**工程教训**：修改接口方法签名后，旧测试字节码仍指向老签名会抛 `NoSuchMethodError`，必须 `mvn clean test`；已在测试流程中固定使用 clean。

---

## 7 安全自查清单（对照实验三遗留问题）

| 实验三遗留问题 | 实验四处理 |
|---|---|
| 注册可指定角色（提权） | 注册接口固定授予 USER 角色，角色由后端查询写入 |
| 密码 MD5 无盐 | 改用 BCrypt（cost=10，自带随机盐） |
| 订单状态可被前端伪造 | 状态流转全部由服务端 CAS 控制，接口不接受状态参数 |
| 越权确认/收货 | Service 内校验参与方身份（买家/卖家），并有单测覆盖 |
| 超卖 | 商品状态 CAS（`ON_SALE→LOCKED`），影响行数为 0 即失败 |
| 负余额 | 扣款条件更新 `balance >= amount` |
| 卖家不结算 | 支付事务内同时给卖家入账并写流水 |
| 评价伪造 | 只有订单参与方、订单已完成才能评价，`uk_order_review_id` 唯一约束 |
| 密码散列泄露 | 所有出参使用 DTO，实体 `password` 永不出现在 JSON 中 |
| 审计日志越权 | 审计查询需 `audit:view`，接口只读，不提供删除 |
| 封禁后会话仍有效 | 每次请求校验用户 `status`，禁用立即生效 |
| CSRF | 令牌放请求头而非 Cookie，关闭 CSRF 不引入风险面 |
| 上传漏洞 | 扩展名白名单 + MIME 校验 + 随机文件名 + 路径归属校验 |
| SQL 注入 | MyBatis 预编译 + 排序字段白名单，禁用 `${}` |
