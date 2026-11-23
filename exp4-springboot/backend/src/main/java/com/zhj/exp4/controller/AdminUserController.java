package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.request.RolePermissionRequest;
import com.zhj.exp4.dto.request.UserQuery;
import com.zhj.exp4.dto.request.UserStatusRequest;
import com.zhj.exp4.dto.response.AdminUserResponse;
import com.zhj.exp4.dto.response.PermissionResponse;
import com.zhj.exp4.dto.response.RoleResponse;
import com.zhj.exp4.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端 · 用户与角色权限。
 *
 * <p>{@code role:manage} 只授予超级管理员：管理员可以管用户，但不能改权限矩阵，
 * 这是最小权限原则的体现。
 */
@Tag(name = "12. 管理-用户与权限", description = "用户列表、启禁用、重置密码、角色权限")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @Operation(summary = "用户分页（含角色）")
    @PreAuthorize("hasAuthority('user:manage')")
    @GetMapping("/users")
    public Result<PageResult<AdminUserResponse>> users(UserQuery query) {
        return Result.ok(adminService.pageUsers(query));
    }

    @Operation(summary = "启用/禁用用户")
    @PreAuthorize("hasAuthority('user:manage')")
    @Audit(module = "用户", action = "启禁用用户")
    @PutMapping("/users/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @Valid @RequestBody UserStatusRequest request) {
        adminService.changeUserStatus(id, request.getStatus());
        return Result.ok(request.getStatus() == 1 ? "账号已启用" : "账号已禁用", null);
    }

    @Operation(summary = "重置用户密码（返回一次性明文）")
    @PreAuthorize("hasAuthority('user:manage')")
    @Audit(module = "用户", action = "重置密码", saveParams = false)
    @PutMapping("/users/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id) {
        String password = adminService.resetPassword(id);
        return Result.ok("密码已重置，请转告用户", Map.of("password", password));
    }

    @Operation(summary = "角色列表（含已分配权限）")
    @PreAuthorize("hasAuthority('role:manage')")
    @GetMapping("/roles")
    public Result<List<RoleResponse>> roles() {
        return Result.ok(adminService.listRoles());
    }

    @Operation(summary = "全量权限清单")
    @PreAuthorize("hasAuthority('role:manage')")
    @GetMapping("/permissions")
    public Result<List<PermissionResponse>> permissions() {
        return Result.ok(adminService.listPermissions());
    }

    @Operation(summary = "重新分配角色权限（全量覆盖）")
    @PreAuthorize("hasAuthority('role:manage')")
    @Audit(module = "用户", action = "分配角色权限")
    @PutMapping("/roles/{id}/permissions")
    public Result<Void> updatePermissions(@PathVariable Long id,
                                          @Valid @RequestBody RolePermissionRequest request) {
        adminService.updateRolePermissions(id, request.getPermissionIds());
        return Result.ok("权限已更新，相关用户刷新后生效", null);
    }
}
