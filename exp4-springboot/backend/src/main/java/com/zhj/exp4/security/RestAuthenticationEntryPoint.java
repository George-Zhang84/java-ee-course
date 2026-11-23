package com.zhj.exp4.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证入口处理：返回 HTTP 401 + 统一响应体（code=1002）。
 *
 * <p>为什么要自定义：Spring Security 默认返回 403 或重定向到登录页，
 * 前后端分离场景下前端需要明确的 JSON 结构才能区分"未登录"与"无权限"。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeJson(response, Result.fail(ResultCode.UNAUTHORIZED));
    }

    private void writeJson(HttpServletResponse response, Result<?> body) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
