package com.zhj.exp4.controller;

import com.zhj.exp4.common.GlobalExceptionHandler;
import com.zhj.exp4.dto.request.LoginRequest;
import com.zhj.exp4.dto.response.LoginResponse;
import com.zhj.exp4.dto.response.UserInfoResponse;
import com.zhj.exp4.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口 Web 层测试（MockMvc standalone，不启动 Spring 容器）。
 *
 * <p>验证三件事：统一响应结构、参数校验错误码、业务异常被全局处理器转换。
 */
@DisplayName("认证接口（Web 层）")
class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("登录成功返回 code=0 与令牌")
    void loginOk() throws Exception {
        when(authService.login(any(LoginRequest.class), anyString(), any())).thenReturn(LoginResponse.builder()
                .token("header.payload.signature")
                .expiresIn(7200)
                .user(UserInfoResponse.builder().id(3L).username("buyer1").nickname("小明").build())
                .permissions(Set.of("order:pay"))
                .roles(List.of("USER"))
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"buyer1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("header.payload.signature"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("用户名为空返回参数错误码 1001")
    void loginBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名")));
    }

    @Test
    @DisplayName("密码过短返回参数错误码 1001")
    void loginShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"buyer1\",\"password\":\"123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("请求体非法 JSON 返回 1001 而不是 500")
    void loginMalformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
