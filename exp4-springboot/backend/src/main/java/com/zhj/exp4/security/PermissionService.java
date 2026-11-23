package com.zhj.exp4.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhj.exp4.entity.SysPermission;
import com.zhj.exp4.entity.SysRole;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.enums.RoleCode;
import com.zhj.exp4.mapper.SysPermissionMapper;
import com.zhj.exp4.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限装配服务（Cache-Aside 模式）。
 *
 * <p>每次请求都联三张表拼权限显然不现实，因此：
 * <ol>
 *   <li>首次访问时从数据库加载角色与权限码，写入 Caffeine 本地缓存（5 分钟过期）；</li>
 *   <li>后续请求命中缓存，零数据库开销；</li>
 *   <li>管理员修改角色权限后调用 {@link #evict(Long)}，让变更立即生效。</li>
 * </ol>
 *
 * <p>之所以用本地缓存而非 Redis：本系统是单体应用，本地缓存已够用，
 * 且不引入额外中间件，符合"非后端分布式系统"的课程要求。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;

    /** userId -> 权限快照；最大 1000 条，写入 5 分钟后过期 */
    private final Cache<Long, AuthInfo> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 加载用户权限快照（优先读缓存）。
     *
     * @param user 已通过密码校验的用户
     * @return 权限快照（含角色与权限码）
     */
    public AuthInfo load(SysUser user) {
        return cache.get(user.getId(), id -> doLoad(user));
    }

    /**
     * 强制刷新：先失效缓存再加载，用于"修改密码后立即刷新权限"等场景。
     */
    public AuthInfo reload(SysUser user) {
        cache.invalidate(user.getId());
        return load(user);
    }

    private AuthInfo doLoad(SysUser user) {
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        List<SysPermission> permissions = permissionMapper.selectByUserId(user.getId());
        Set<String> codes = permissions.stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toUnmodifiableSet());
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode)
                .toList();
        return AuthInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(roleCodes)
                .permissions(codes)
                .build();
    }

    /** 权限变更后清理单个用户的缓存 */
    public void evict(Long userId) {
        cache.invalidate(userId);
    }

    /** 角色-权限矩阵变更时全量清理（影响面大，但变更频率低） */
    public void evictAll() {
        cache.invalidateAll();
    }

    /** 是否具备某权限码 */
    public boolean hasPermission(AuthInfo authInfo, String code) {
        return authInfo.getPermissions().contains(code);
    }

    /** 是否超级管理员：用于"超管直接放行"的兜底判断 */
    public boolean isSuperAdmin(AuthInfo authInfo) {
        return authInfo.getRoles().contains(RoleCode.SUPER_ADMIN.name());
    }
}
