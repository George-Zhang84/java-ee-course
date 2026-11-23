package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.dto.request.AuditLogQuery;
import com.zhj.exp4.dto.request.ProductAuditRequest;
import com.zhj.exp4.dto.request.UserQuery;
import com.zhj.exp4.dto.response.AdminUserResponse;
import com.zhj.exp4.dto.response.AuditLogResponse;
import com.zhj.exp4.dto.response.CategoryStat;
import com.zhj.exp4.dto.response.DashboardSummary;
import com.zhj.exp4.dto.response.HotProduct;
import com.zhj.exp4.dto.response.LoginLogResponse;
import com.zhj.exp4.dto.response.PermissionResponse;
import com.zhj.exp4.dto.response.ProductListResponse;
import com.zhj.exp4.dto.response.RoleResponse;
import com.zhj.exp4.dto.response.TrendPoint;
import com.zhj.exp4.entity.AuditLog;
import com.zhj.exp4.entity.Category;
import com.zhj.exp4.entity.LoginLog;
import com.zhj.exp4.entity.Product;
import com.zhj.exp4.entity.SysPermission;
import com.zhj.exp4.entity.SysRole;
import com.zhj.exp4.entity.SysRolePermission;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.entity.SysUserRole;
import com.zhj.exp4.enums.ProductStatus;
import com.zhj.exp4.enums.RoleCode;
import com.zhj.exp4.mapper.AuditLogMapper;
import com.zhj.exp4.mapper.CategoryMapper;
import com.zhj.exp4.mapper.LoginLogMapper;
import com.zhj.exp4.mapper.ProductMapper;
import com.zhj.exp4.mapper.StatisticsMapper;
import com.zhj.exp4.mapper.SysPermissionMapper;
import com.zhj.exp4.mapper.SysRoleMapper;
import com.zhj.exp4.mapper.SysRolePermissionMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.mapper.SysUserRoleMapper;
import com.zhj.exp4.security.PermissionService;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.AdminService;
import com.zhj.exp4.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端服务实现。
 *
 * <p>安全红线（都在这里显式实现，不依赖前端隐藏按钮）：
 * <ol>
 *   <li>不能修改自己的账号状态，避免管理员误封自己；</li>
 *   <li>不能禁用超级管理员；</li>
 *   <li>不能修改超级管理员的角色权限，避免把系统锁死；</li>
 *   <li>删除角色权限或禁用用户后必须清理权限缓存，否则最长 5 分钟才生效。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final AuditLogMapper auditLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final StatisticsMapper statisticsMapper;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------
    // 商品审核
    // ------------------------------------------------------------------

    @Override
    public PageResult<ProductListResponse> pageProducts(String status, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = Wrappers.lambdaQuery(Product.class);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Product::getStatus, status.trim());
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)), wrapper);
        List<Product> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        Map<Long, String> sellers = batchSellerNames(records.stream().map(Product::getSellerId).toList());
        Map<Long, String> categories = batchCategoryNames(records.stream().map(Product::getCategoryId).toList());
        return PageResult.of(result, p -> ProductListResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .price(p.getPrice())
                .coverUrl(p.getCoverUrl())
                .conditionLevel(p.getConditionLevel())
                .location(p.getLocation())
                .categoryId(p.getCategoryId())
                .categoryName(categories.get(p.getCategoryId()))
                .sellerId(p.getSellerId())
                .sellerNickname(sellers.get(p.getSellerId()))
                .allowBargain(p.getAllowBargain())
                .status(p.getStatus())
                .statusText(statusText(p.getStatus()))
                .views(p.getViews())
                .createTime(p.getCreateTime())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(Long productId, ProductAuditRequest request) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (!ProductStatus.PENDING.name().equals(product.getStatus())) {
            throw BizException.stateConflict("只有待审核商品可以审核");
        }
        if (!request.getPass() && !StringUtils.hasText(request.getRemark())) {
            throw BizException.of("驳回时请填写驳回原因");
        }

        String target = request.getPass()
                ? ProductStatus.ON_SALE.name()
                : ProductStatus.REJECTED.name();
        int rows = productMapper.casStatus(productId, ProductStatus.PENDING.name(), target);
        if (rows == 0) {
            throw BizException.stateConflict("商品状态已变更，请刷新后重试");
        }
        // 审核意见单独写入（CAS 只负责状态原子性）。
        // 注意：这里必须用 UpdateWrapper 显式 set，不能用 updateById —— 当 remark 为 null 时
        // updateById 会生成没有 SET 子句的 SQL（"UPDATE product WHERE id=?"）直接报错，
        // 表现为"不填审核意见 -> 系统繁忙"。显式 set 允许把字段置为 NULL。
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .eq(Product::getId, productId)
                .set(Product::getAuditRemark, request.getRemark()));

        String title = request.getPass() ? "商品审核通过" : "商品审核未通过";
        String content = request.getPass()
                ? "你的商品「" + product.getTitle() + "」已上架"
                : "你的商品「" + product.getTitle() + "」被驳回：" + request.getRemark();
        notificationService.send(product.getSellerId(), "AUDIT", title, content, productId);
        log.info("商品审核 productId={} pass={} operator={}", productId, request.getPass(),
                SecurityUtils.currentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceOffline(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        int rows = productMapper.casStatus(productId,
                ProductStatus.ON_SALE.name(), ProductStatus.OFF_SHELF.name());
        if (rows == 0) {
            throw BizException.stateConflict("只有在售商品可以强制下架");
        }
        notificationService.send(product.getSellerId(), "AUDIT", "商品被强制下架",
                "你的商品「" + product.getTitle() + "」已被管理员下架，如有疑问请联系管理员", productId);
    }

    // ------------------------------------------------------------------
    // 用户管理
    // ------------------------------------------------------------------

    @Override
    public PageResult<AdminUserResponse> pageUsers(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class);
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getNickname, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(
                Page.of(query.safePage(), query.safeSize()), wrapper);

        List<SysUser> records = result.getRecords();
        Map<Long, List<String>> rolesMap = batchRoles(records.stream().map(SysUser::getId).toList());
        return PageResult.of(result, u -> AdminUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .phone(u.getPhone())
                .email(u.getEmail())
                .balance(u.getBalance())
                .creditScore(u.getCreditScore())
                .status(u.getStatus())
                .roles(rolesMap.getOrDefault(u.getId(), List.of()))
                .createTime(u.getCreateTime())
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUserStatus(Long userId, Integer status) {
        Long currentUserId = SecurityUtils.currentUserId();
        if (currentUserId.equals(userId)) {
            throw BizException.of("不能修改自己的账号状态");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        List<String> roles = batchRoles(List.of(userId)).getOrDefault(userId, List.of());
        if (roles.contains(RoleCode.SUPER_ADMIN.name())) {
            throw BizException.of("不能禁用超级管理员");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
        // 立即刷新权限缓存，禁用后下一次请求就会被拒绝
        permissionService.evict(userId);
        log.info("用户状态变更 userId={} status={} operator={}", userId, status, SecurityUtils.currentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        String plain = randomPassword(8);
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(plain));
        userMapper.updateById(update);
        log.info("管理员重置密码 userId={} operator={}", userId, SecurityUtils.currentUsername());
        return plain;
    }

    // ------------------------------------------------------------------
    // 角色与权限
    // ------------------------------------------------------------------

    @Override
    public List<RoleResponse> listRoles() {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
        List<SysRolePermission> relations = rolePermissionMapper.selectList(null);
        Map<Long, List<Long>> map = new HashMap<>();
        for (SysRolePermission relation : relations) {
            map.computeIfAbsent(relation.getRoleId(), k -> new ArrayList<>()).add(relation.getPermissionId());
        }
        return roles.stream().map(r -> RoleResponse.builder()
                .id(r.getId())
                .code(r.getCode())
                .name(r.getName())
                .description(r.getDescription())
                .permissionIds(map.getOrDefault(r.getId(), List.of()))
                .build()).toList();
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery().orderByAsc(SysPermission::getSort))
                .stream().map(PermissionResponse::of).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (RoleCode.SUPER_ADMIN.name().equals(role.getCode())) {
            throw BizException.of("超级管理员权限不可修改，避免系统被锁死");
        }
        // 全量覆盖：先删后插，保证与前端勾选结果完全一致
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds != null) {
            for (Long permissionId : permissionIds.stream().filter(Objects::nonNull).distinct().toList()) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                rolePermissionMapper.insert(relation);
            }
        }
        // 权限矩阵变了，所有用户的权限缓存都要失效
        permissionService.evictAll();
        log.info("角色权限更新 roleId={} 权限数={}", roleId, permissionIds == null ? 0 : permissionIds.size());
    }

    // ------------------------------------------------------------------
    // 审计与日志
    // ------------------------------------------------------------------

    @Override
    public PageResult<AuditLogResponse> pageAuditLogs(AuditLogQuery query) {
        LambdaQueryWrapper<AuditLog> wrapper = Wrappers.lambdaQuery(AuditLog.class);
        if (StringUtils.hasText(query.getModule())) {
            wrapper.eq(AuditLog::getModule, query.getModule().trim());
        }
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(AuditLog::getUsername, query.getUsername().trim());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(AuditLog::getCreateTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            // 结束日期当天也要包含：用 < 次日 0 点
            wrapper.lt(AuditLog::getCreateTime, query.getEndDate().plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(AuditLog::getCreateTime);
        Page<AuditLog> result = auditLogMapper.selectPage(
                Page.of(query.safePage(), query.safeSize()), wrapper);
        return PageResult.of(result, a -> AuditLogResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .username(a.getUsername())
                .module(a.getModule())
                .action(a.getAction())
                .method(a.getMethod())
                .uri(a.getUri())
                .ip(a.getIp())
                .params(a.getParams())
                .result(a.getResult())
                .costMs(a.getCostMs())
                .createTime(a.getCreateTime())
                .build());
    }

    @Override
    public PageResult<LoginLogResponse> pageLoginLogs(String username, int page, int size) {
        LambdaQueryWrapper<LoginLog> wrapper = Wrappers.lambdaQuery(LoginLog.class);
        if (StringUtils.hasText(username)) {
            wrapper.like(LoginLog::getUsername, username.trim());
        }
        wrapper.orderByDesc(LoginLog::getCreateTime);
        Page<LoginLog> result = loginLogMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)), wrapper);
        return PageResult.of(result, l -> LoginLogResponse.builder()
                .id(l.getId())
                .userId(l.getUserId())
                .username(l.getUsername())
                .ip(l.getIp())
                .userAgent(l.getUserAgent())
                .success(l.getSuccess())
                .msg(l.getMsg())
                .createTime(l.getCreateTime())
                .build());
    }

    // ------------------------------------------------------------------
    // 数据看板
    // ------------------------------------------------------------------

    @Override
    public DashboardSummary summary() {
        return DashboardSummary.builder()
                .userCount(statisticsMapper.countUsers())
                .onSaleCount(statisticsMapper.countOnSaleProducts())
                .pendingCount(statisticsMapper.countPendingProducts())
                .orderCount(statisticsMapper.countOrders())
                .gmv(defaultZero(statisticsMapper.sumGmv()))
                .todayOrders(statisticsMapper.countTodayOrders())
                .todayGmv(defaultZero(statisticsMapper.sumTodayGmv()))
                .build();
    }

    @Override
    public List<TrendPoint> trend(int days) {
        int span = Math.min(Math.max(days, 1), 30);
        LocalDate start = LocalDate.now().minusDays(span - 1L);
        Map<LocalDate, TrendPoint> dbMap = statisticsMapper.trendSince(start.atStartOfDay()).stream()
                .collect(Collectors.toMap(TrendPoint::getStatDate, p -> p, (a, b) -> a));
        // 补齐没有订单的日期，前端画图时 X 轴才连续
        List<TrendPoint> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate date = start.plusDays(i);
            TrendPoint point = dbMap.get(date);
            if (point == null) {
                point = new TrendPoint();
                point.setStatDate(date);
                point.setOrderCount(0L);
                point.setGmv(BigDecimal.ZERO);
            }
            result.add(point);
        }
        return result;
    }

    @Override
    public List<CategoryStat> categoryStats() {
        return statisticsMapper.categoryStats();
    }

    @Override
    public List<HotProduct> hotProducts(int limit) {
        return statisticsMapper.hotProducts(Math.min(Math.max(limit, 1), 20));
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    private Map<Long, List<String>> batchRoles(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> relations = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, ids));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> roleNames = roleMapper.selectBatchIds(
                        relations.stream().map(SysUserRole::getRoleId).distinct().toList())
                .stream().collect(Collectors.toMap(SysRole::getId, SysRole::getCode));
        Map<Long, List<String>> result = new HashMap<>();
        for (SysUserRole relation : relations) {
            String code = roleNames.get(relation.getRoleId());
            if (code != null) {
                result.computeIfAbsent(relation.getUserId(), k -> new ArrayList<>()).add(code);
            }
        }
        return result;
    }

    private Map<Long, String> batchSellerNames(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
    }

    private Map<Long, String> batchCategoryNames(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private String statusText(String status) {
        try {
            return ProductStatus.valueOf(status).getDescription();
        } catch (Exception e) {
            return status;
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 生成随机密码：去掉易混淆字符（0/O、1/l/I） */
    private String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
