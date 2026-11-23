package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.dto.request.ChangePasswordRequest;
import com.zhj.exp4.dto.request.LoginRequest;
import com.zhj.exp4.dto.request.RegisterRequest;
import com.zhj.exp4.dto.response.LoginResponse;
import com.zhj.exp4.dto.response.UserInfoResponse;
import com.zhj.exp4.entity.LoginLog;
import com.zhj.exp4.entity.SysRole;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.entity.SysUserRole;
import com.zhj.exp4.enums.RoleCode;
import com.zhj.exp4.mapper.LoginLogMapper;
import com.zhj.exp4.mapper.SysRoleMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.mapper.SysUserRoleMapper;
import com.zhj.exp4.security.AuthInfo;
import com.zhj.exp4.security.JwtUtil;
import com.zhj.exp4.security.LoginUser;
import com.zhj.exp4.security.PermissionService;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.security.TokenBlacklist;
import com.zhj.exp4.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 认证服务实现。
 *
 * <p>登录流程中的安全考量：
 * <ol>
 *   <li><b>错误信息统一</b>：用户名不存在与密码错误返回同一句提示，避免被枚举账号；</li>
 *   <li><b>失败次数限制</b>：5 分钟内同一用户名失败 5 次即拒绝，抵御暴力破解；</li>
 *   <li><b>BCrypt 校验</b>：数据库存密文，使用带盐的 BCrypt 比对，杜绝彩虹表；</li>
 *   <li><b>禁用账号拦截</b>：status=0 的账号即使密码正确也不签发令牌。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 5 分钟内允许的连续失败次数 */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    /** 失败统计窗口（分钟） */
    private static final int FAILED_WINDOW_MINUTES = 5;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;
    private final TokenBlacklist tokenBlacklist;
    private final AppProperties appProperties;

    @Override
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        String username = request.getUsername().trim();
        checkLoginAttempts(username);

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));

        // 用户不存在与密码错误返回同一提示，防止账号枚举
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginLog(user == null ? null : user.getId(), username, ip, userAgent, false, "用户名或密码错误");
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            recordLoginLog(user.getId(), username, ip, userAgent, false, "账号已被禁用");
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        recordLoginLog(user.getId(), username, ip, userAgent, true, "登录成功");
        return buildLoginResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        Long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw BizException.of("用户名已被占用");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setBalance(BigDecimal.ZERO);
        user.setCreditScore(100);
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);

        // 注册只授予 USER 角色，角色由后端决定，不接受前端传参（修复实验三注册提权漏洞）
        SysRole userRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, RoleCode.USER.name()));
        if (userRole == null) {
            throw BizException.of("系统角色数据缺失，请联系管理员");
        }
        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(userRole.getId());
        userRoleMapper.insert(relation);

        log.info("新用户注册成功 username={} userId={}", username, user.getId());
        return buildLoginResponse(user);
    }

    @Override
    public UserInfoResponse currentUser() {
        LoginUser loginUser = SecurityUtils.currentUser();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        UserInfoResponse response = UserInfoResponse.of(user);
        response.setRoles(loginUser.getRoles());
        response.setPermissions(permissionService.load(user).getPermissions());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request, String token) {
        LoginUser loginUser = SecurityUtils.currentUser();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BizException.of("原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw BizException.of("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);

        // 改密后旧令牌立即失效，强制重新登录
        revokeQuietly(token);
    }

    @Override
    public void logout(String token) {
        revokeQuietly(token);
    }

    /** 构造登录响应：签发令牌 + 装配权限 */
    private LoginResponse buildLoginResponse(SysUser user) {
        AuthInfo authInfo = permissionService.reload(user);
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        UserInfoResponse userInfo = UserInfoResponse.of(user);
        userInfo.setRoles(authInfo.getRoles());
        userInfo.setPermissions(authInfo.getPermissions());
        return LoginResponse.builder()
                .token(token)
                .expiresIn(appProperties.getJwt().getExpireSeconds())
                .user(userInfo)
                .permissions(authInfo.getPermissions())
                .roles(authInfo.getRoles())
                .build();
    }

    /** 统计窗口内失败次数，超过阈值直接拒绝（不再校验密码，减少 BCrypt 计算开销） */
    private void checkLoginAttempts(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(FAILED_WINDOW_MINUTES);
        Long failed = loginLogMapper.selectCount(Wrappers.<LoginLog>lambdaQuery()
                .eq(LoginLog::getUsername, username)
                .eq(LoginLog::getSuccess, 0)
                .ge(LoginLog::getCreateTime, since));
        if (failed != null && failed >= MAX_FAILED_ATTEMPTS) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS,
                    "登录失败次数过多，请 " + FAILED_WINDOW_MINUTES + " 分钟后再试");
        }
    }

    private void recordLoginLog(Long userId, String username, String ip, String userAgent,
                                boolean success, String msg) {
        LoginLog logEntity = new LoginLog();
        logEntity.setUserId(userId);
        logEntity.setUsername(username);
        logEntity.setIp(ip);
        logEntity.setUserAgent(userAgent != null && userAgent.length() > 255
                ? userAgent.substring(0, 255) : userAgent);
        logEntity.setSuccess(success ? 1 : 0);
        logEntity.setMsg(msg);
        loginLogMapper.insert(logEntity);
    }

    /** 令牌吊销是尽力而为：解析失败（已过期/格式错）不影响主流程 */
    private void revokeQuietly(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            tokenBlacklist.revoke(token, jwtUtil.expirationOf(claims));
        } catch (Exception e) {
            log.debug("令牌吊销跳过：{}", e.getMessage());
        }
    }
}
