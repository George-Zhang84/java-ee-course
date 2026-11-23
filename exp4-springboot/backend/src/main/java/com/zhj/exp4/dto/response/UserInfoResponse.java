package com.zhj.exp4.dto.response;

import com.zhj.exp4.entity.SysUser;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 用户信息响应（脱敏后的对外视图）。
 *
 * <p>与实体 {@code SysUser} 分离的意义：实体含 {@code password}，
 * 直接返回会把密文泄露给前端（实验三曾出现该问题）。
 * 这里只挑允许对外暴露的字段。
 */
@Data
@Builder
public class UserInfoResponse {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private BigDecimal balance;
    private Integer creditScore;
    private Integer status;
    private List<String> roles;
    private Set<String> permissions;

    /** 由实体转换，敏感字段（password/deleted）不下发 */
    public static UserInfoResponse of(SysUser user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .balance(user.getBalance())
                .creditScore(user.getCreditScore())
                .status(user.getStatus())
                .build();
    }
}
