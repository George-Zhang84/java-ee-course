package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户表实体（sys_user）。
 *
 * <p>注意：{@code password} 存的是 BCrypt 密文，仅在认证流程内部使用，
 * 绝不允许直接序列化返回给前端——对外一律走 {@code UserInfoResponse}。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密文，$2a$10$ 前缀，不可逆 */
    private String password;

    private String nickname;

    private String phone;

    private String email;

    private String avatar;

    /** 钱包余额，DECIMAL 对应 BigDecimal */
    private BigDecimal balance;

    /** 信用分，初始 100，按评价动态调整 */
    private Integer creditScore;

    /** 1 正常 / 0 禁用，禁用后登录与令牌校验均拒绝 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除标记，配合 yml 中 logic-delete-field 使用 */
    @TableLogic
    private Integer deleted;
}
