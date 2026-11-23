package com.zhj.exp4.service;

import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.dto.request.LoginRequest;
import com.zhj.exp4.dto.response.LoginResponse;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.mapper.LoginLogMapper;
import com.zhj.exp4.mapper.SysRoleMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.mapper.SysUserRoleMapper;
import com.zhj.exp4.security.AuthInfo;
import com.zhj.exp4.security.JwtUtil;
import com.zhj.exp4.security.PermissionService;
import com.zhj.exp4.security.TokenBlacklist;
import com.zhj.exp4.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务单元测试。
 *
 * <p>覆盖登录的四道关：失败次数限制、密码校验、禁用账号、成功签发。
 * 其中"用户名不存在与密码错误返回同一提示"是防止账号枚举的安全要求。
 */
@DisplayName("登录认证与风控")
class AuthServiceImplTest {

    private SysUserMapper userMapper;
    private LoginLogMapper loginLogMapper;
    private PermissionService permissionService;
    private AuthServiceImpl authService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        userMapper = Mockito.mock(SysUserMapper.class);
        SysRoleMapper roleMapper = Mockito.mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = Mockito.mock(SysUserRoleMapper.class);
        loginLogMapper = Mockito.mock(LoginLogMapper.class);
        permissionService = Mockito.mock(PermissionService.class);

        AppProperties props = new AppProperties();
        props.getJwt().setSecret("exp4-auth-test-secret-0123456789abcdef");
        props.getJwt().setExpireSeconds(3600);
        JwtUtil jwtUtil = new JwtUtil(props);

        authService = new AuthServiceImpl(userMapper, roleMapper, userRoleMapper, loginLogMapper,
                encoder, jwtUtil, permissionService, new TokenBlacklist(), props);
    }

    @Test
    @DisplayName("登录成功：返回令牌、角色与权限")
    void loginSuccess() {
        when(loginLogMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectOne(any())).thenReturn(user("buyer1", "123456", 1));
        when(permissionService.reload(any())).thenReturn(AuthInfo.builder()
                .userId(3L).username("buyer1").nickname("小明")
                .roles(List.of("USER"))
                .permissions(Set.of("order:pay", "menu:product"))
                .build());

        LoginResponse res = authService.login(request("buyer1", "123456"), "127.0.0.1", "JUnit");

        assertNotNull(res.getToken());
        assertEquals(3, res.getToken().split("\\.").length);
        assertEquals("buyer1", res.getUser().getUsername());
        assertEquals(2, res.getPermissions().size());
        assertEquals(List.of("USER"), res.getRoles());
        verify(loginLogMapper).insert(any(com.zhj.exp4.entity.LoginLog.class));
    }

    @Test
    @DisplayName("密码错误：统一提示且写失败日志")
    void loginWrongPassword() {
        when(loginLogMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectOne(any())).thenReturn(user("buyer1", "123456", 1));

        BizException ex = assertThrows(BizException.class,
                () -> authService.login(request("buyer1", "wrong"), "127.0.0.1", "JUnit"));

        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        assertEquals("用户名或密码错误", ex.getMessage());
        verify(permissionService, never()).reload(any());
    }

    @Test
    @DisplayName("用户不存在：与密码错误同一提示（防账号枚举）")
    void loginUnknownUser() {
        when(loginLogMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectOne(any())).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> authService.login(request("ghost", "123456"), "127.0.0.1", "JUnit"));

        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("禁用账号即使密码正确也拒绝登录")
    void loginDisabledUser() {
        when(loginLogMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectOne(any())).thenReturn(user("buyer1", "123456", 0));

        BizException ex = assertThrows(BizException.class,
                () -> authService.login(request("buyer1", "123456"), "127.0.0.1", "JUnit"));

        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(permissionService, never()).reload(any());
    }

    @Test
    @DisplayName("5 分钟内失败 5 次后直接限流，不再计算密码")
    void loginRateLimited() {
        when(loginLogMapper.selectCount(any())).thenReturn(5L);

        BizException ex = assertThrows(BizException.class,
                () -> authService.login(request("buyer1", "123456"), "127.0.0.1", "JUnit"));

        assertEquals(ResultCode.TOO_MANY_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("分钟"));
        verify(userMapper, never()).selectOne(any());
    }

    // ------------------------------------------------------------------

    private LoginRequest request(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private SysUser user(String username, String rawPassword, int status) {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername(username);
        user.setNickname("小明");
        user.setPassword(encoder.encode(rawPassword));
        user.setStatus(status);
        user.setBalance(new BigDecimal("1000.00"));
        user.setCreditScore(100);
        return user;
    }
}
