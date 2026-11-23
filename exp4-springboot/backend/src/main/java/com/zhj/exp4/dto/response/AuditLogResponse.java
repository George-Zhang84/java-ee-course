package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志响应。
 */
@Data
@Builder
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String method;
    private String uri;
    private String ip;
    private String params;
    private String result;
    private Integer costMs;
    private LocalDateTime createTime;
}
