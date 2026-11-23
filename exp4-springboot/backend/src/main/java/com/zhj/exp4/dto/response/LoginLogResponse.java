package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志响应。
 */
@Data
@Builder
public class LoginLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private Integer success;
    private String msg;
    private LocalDateTime createTime;
}
