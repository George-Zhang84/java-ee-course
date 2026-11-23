package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体（login_log）。成功与失败都记录，
 * 用于"登录失败次数限制"和异常登录排查。
 */
@Data
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录失败时可能为空（用户名不存在） */
    private Long userId;

    private String username;

    private String ip;

    private String userAgent;

    /** 1 成功 / 0 失败 */
    private Integer success;

    private String msg;

    private LocalDateTime createTime;
}
