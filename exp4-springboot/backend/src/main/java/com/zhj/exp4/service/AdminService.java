package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
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

import java.util.List;

/**
 * 管理端服务：商品审核、用户管理、角色权限、审计查询、数据看板。
 *
 * <p>把后台功能集中在同一服务，是为了让"管理员能力"有清晰边界：
 * 前台买/卖相关逻辑都在 ProductService / OrderService / BargainService 里，
 * 后台运营相关逻辑都在这里，便于按角色做权限审计。
 */
public interface AdminService {

    /** 商品列表（可按状态筛选，默认待审核） */
    PageResult<ProductListResponse> pageProducts(String status, int page, int size);

    /** 审核商品：通过则上架，驳回则记录原因并通知卖家 */
    void auditProduct(Long productId, ProductAuditRequest request);

    /** 强制下架商品并通知卖家 */
    void forceOffline(Long productId);

    /** 用户分页（含角色） */
    PageResult<AdminUserResponse> pageUsers(UserQuery query);

    /** 启用/禁用用户 */
    void changeUserStatus(Long userId, Integer status);

    /**
     * 重置用户密码。
     *
     * @return 新的明文密码（仅本次返回，管理员需转告用户）
     */
    String resetPassword(Long userId);

    /** 角色列表（含已分配权限 id） */
    List<RoleResponse> listRoles();

    /** 全量权限清单 */
    List<PermissionResponse> listPermissions();

    /** 重新分配角色权限（全量覆盖） */
    void updateRolePermissions(Long roleId, List<Long> permissionIds);

    /** 操作审计日志分页 */
    PageResult<AuditLogResponse> pageAuditLogs(AuditLogQuery query);

    /** 登录日志分页 */
    PageResult<LoginLogResponse> pageLoginLogs(String username, int page, int size);

    /** 看板汇总指标 */
    DashboardSummary summary();

    /** 近 N 日趋势（缺失日期补 0） */
    List<TrendPoint> trend(int days);

    /** 分类商品占比 */
    List<CategoryStat> categoryStats();

    /** 热销商品 TopN */
    List<HotProduct> hotProducts(int limit);
}
