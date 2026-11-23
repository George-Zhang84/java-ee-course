package com.zhj.exp4.security;

import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类。
 *
 * <p>Service 层做数据级越权校验时需要频繁获取"当前用户 id"，
 * 统一从这里取，避免每个类都写一遍 SecurityContextHolder 模板代码。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户。
     *
     * @return 登录主体
     * @throws BizException 未登录时抛出（正常流程下 Controller 之前已被 Security 拦截，此分支主要防定时任务/异步线程误用）
     */
    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /** 当前用户 id */
    public static Long currentUserId() {
        return currentUser().getUserId();
    }

    /** 当前用户名 */
    public static String currentUsername() {
        return currentUser().getUsername();
    }

    /** 当前用户是否具备某权限码（用于 Service 内部二次判断） */
    public static boolean hasPermission(String code) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> code.equals(a.getAuthority()));
    }

    /**
     * 校验资源归属：不是本人且不是超级管理员则拒绝。
     *
     * <p>这是防止"改个 URL 就能操作别人订单"的水平越权关键防线，
     * 不能只依赖前端按钮隐藏。
     *
     * @param ownerId 资源拥有者 id
     */
    public static void checkOwnership(Long ownerId) {
        LoginUser user = currentUser();
        if (!user.getUserId().equals(ownerId) && !user.hasRole("SUPER_ADMIN")) {
            throw BizException.ownershipDenied();
        }
    }
}
