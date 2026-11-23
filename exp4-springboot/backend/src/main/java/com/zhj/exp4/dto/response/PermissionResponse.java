package com.zhj.exp4.dto.response;

import com.zhj.exp4.entity.SysPermission;
import lombok.Builder;
import lombok.Data;

/**
 * 权限项响应。后端返回全量权限清单，前端按 type 分组展示（菜单/按钮/接口）。
 */
@Data
@Builder
public class PermissionResponse {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private Integer sort;

    public static PermissionResponse of(SysPermission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .type(p.getType())
                .parentId(p.getParentId())
                .sort(p.getSort())
                .build();
    }
}
