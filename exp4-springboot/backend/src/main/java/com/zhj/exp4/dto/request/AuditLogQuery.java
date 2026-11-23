package com.zhj.exp4.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 审计日志查询参数。
 *
 * <p>时间范围用 {@code LocalDate} 接收（前端只选日期），
 * 服务端把结束日期加一天再比较，保证"当天"的记录也被包含。
 */
@Data
public class AuditLogQuery {

    @Schema(description = "业务模块")
    private String module;

    @Schema(description = "操作人用户名")
    private String username;

    @Schema(description = "开始日期 yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期 yyyy-MM-dd")
    private LocalDate endDate;

    private Integer page = 1;

    private Integer size = 10;

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int safeSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }
}
