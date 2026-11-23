package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户列表项（含角色，不含密码）。
 */
@Data
@Builder
public class AdminUserResponse {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private BigDecimal balance;
    private Integer creditScore;
    private Integer status;
    private List<String> roles;
    private LocalDateTime createTime;
}
