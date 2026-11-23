package com.zhj.exp4.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 角色（含已分配权限 id 列表），供后台权限矩阵页面回显勾选状态。
 */
@Data
@Builder
public class RoleResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private List<Long> permissionIds;
}
