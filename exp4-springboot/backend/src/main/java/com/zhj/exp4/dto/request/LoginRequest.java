package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求。
 *
 * <p>只做"非空 + 长度"这类基本校验；长度上限同时起到防止超长字符串攻击的作用。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名长度不能超过 32")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6~32 之间")
    private String password;
}
