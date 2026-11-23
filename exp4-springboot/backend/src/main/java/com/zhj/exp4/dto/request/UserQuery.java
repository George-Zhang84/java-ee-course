package com.zhj.exp4.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户管理查询参数。
 */
@Data
public class UserQuery {

    @Schema(description = "关键词，匹配用户名或昵称")
    private String keyword;

    @Schema(description = "状态筛选：1 正常 / 0 禁用")
    private Integer status;

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
