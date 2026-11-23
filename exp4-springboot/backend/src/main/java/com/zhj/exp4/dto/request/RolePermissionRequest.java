package com.zhj.exp4.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色权限分配请求：全量覆盖该角色的权限集合。
 *
 * <p>采用"全量覆盖"而非增删单条，前端提交勾选后的完整 id 列表，
 * 服务端先删后插，避免出现"两边各改一半"的不一致。
 */
@Data
public class RolePermissionRequest {

    @NotNull(message = "权限列表不能为空")
    private List<Long> permissionIds;
}
