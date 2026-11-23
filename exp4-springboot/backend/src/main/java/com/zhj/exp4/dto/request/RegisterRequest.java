package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求。
 *
 * <p>注意：注册一律只授予 USER 角色，绝不允许前端传角色字段——
 * 这是实验三"注册提权漏洞"的修复点（当时可在注册表单里指定 role）。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$",
            message = "用户名需以字母开头，由字母/数字/下划线组成，长度 4~20")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6~32 之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称长度不能超过 32")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64")
    private String email;
}
