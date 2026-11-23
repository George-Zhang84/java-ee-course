package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知响应。
 */
@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Long bizId;
    private Integer readFlag;
    private LocalDateTime createTime;
}
