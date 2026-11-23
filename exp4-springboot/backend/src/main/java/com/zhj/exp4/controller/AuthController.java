package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.ChangePasswordRequest;
import com.zhj.exp4.dto.request.LoginRequest;
import com.zhj.exp4.dto.request.RegisterRequest;
import com.zhj.exp4.dto.response.LoginResponse;
import com.zhj.exp4.dto.response.UserInfoResponse;
import com.zhj.exp4.service.AuthService;
import com.zhj.exp4.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * <p>Controller 的职责边界：只做"取参 → 调 Service → 包 Result"，
 * 不写业务判断，保证业务可被单测与复用（定时任务、其他 Service 也能调）。
 */
@Tag(name = "01. 认证", description = "注册、登录、退出、当前用户、修改密码")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    // 登录不写 @Audit：认证事件统一由 login_log 记录（含失败原因、UA），避免审计表出现 "anonymous 认证/登录" 的重复记录
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest servletRequest) {
        String ip = IpUtil.getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        return Result.ok(authService.login(request, ip, userAgent));
    }

    @Operation(summary = "注册（默认普通用户）")
    @Audit(module = "认证", action = "注册", saveParams = false)
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok("注册成功", authService.register(request));
    }

    @Operation(summary = "退出登录")
    @Audit(module = "认证", action = "退出登录", saveParams = false)
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return Result.ok("已退出登录", null);
    }

    @Operation(summary = "当前登录用户信息（含角色与权限码）")
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.currentUser());
    }

    @Operation(summary = "修改密码")
    @Audit(module = "认证", action = "修改密码", saveParams = false)
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.changePassword(request, extractToken(authorization));
        return Result.ok("密码修改成功，请重新登录", null);
    }

    /** 从 Authorization 头中剥离 Bearer 前缀 */
    private String extractToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        return authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()).trim()
                : authorization.trim();
    }
}
