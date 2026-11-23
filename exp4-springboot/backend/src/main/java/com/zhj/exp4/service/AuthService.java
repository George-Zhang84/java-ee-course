package com.zhj.exp4.service;

import com.zhj.exp4.dto.request.ChangePasswordRequest;
import com.zhj.exp4.dto.request.LoginRequest;
import com.zhj.exp4.dto.request.RegisterRequest;
import com.zhj.exp4.dto.response.LoginResponse;
import com.zhj.exp4.dto.response.UserInfoResponse;

/**
 * 认证与账号服务。
 *
 * <p>接口与实现分离的目的：Controller 只依赖接口，
 * 便于后续替换实现（如接入统一认证中心）或写单元测试时打桩。
 */
public interface AuthService {

    /**
     * 登录：校验密码、写登录日志、签发 JWT。
     *
     * @param request   登录参数
     * @param ip        客户端 IP（写日志用）
     * @param userAgent 浏览器 UA（写日志用）
     * @return 令牌与用户权限信息
     */
    LoginResponse login(LoginRequest request, String ip, String userAgent);

    /**
     * 注册：默认授予 USER 角色，注册成功直接返回登录态。
     *
     * @param request 注册参数
     * @return 令牌与用户权限信息
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 查询当前登录用户信息（含角色与权限码）。
     *
     * @return 脱敏后的用户信息
     */
    UserInfoResponse currentUser();

    /**
     * 修改密码：校验旧密码，成功后吊销当前令牌。
     *
     * @param request 修改密码参数
     * @param token   当前请求携带的令牌（用于吊销）
     */
    void changePassword(ChangePasswordRequest request, String token);

    /**
     * 退出登录：把当前令牌加入黑名单。
     *
     * @param token 当前请求携带的令牌
     */
    void logout(String token);
}
