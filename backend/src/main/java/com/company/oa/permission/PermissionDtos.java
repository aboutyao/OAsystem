package com.company.oa.permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class PermissionDtos {
    private PermissionDtos() {
    }

    public record RoleCreateRequest(
            @NotBlank @Size(max = 64) String roleCode,
            @NotBlank @Size(max = 128) String roleName,
            @NotBlank @Size(max = 32) String roleType,
            String status,
            Integer sortOrder,
            String remark
    ) {
    }

    public record RoleUpdateRequest(
            @NotBlank @Size(max = 64) String roleCode,
            @NotBlank @Size(max = 128) String roleName,
            @NotBlank @Size(max = 32) String roleType,
            String status,
            Integer sortOrder,
            String remark
    ) {
    }

    public record AssignMenusRequest(@NotNull List<Long> menuIds) {
    }

    public record AssignButtonsRequest(@NotNull List<Long> buttonIds) {
    }

    public record DataScopeItem(
            @NotBlank @Size(max = 32) String scopeType,
            @NotBlank @Size(max = 64) String businessType,
            List<Long> deptIds
    ) {
    }

    public record AssignDataScopesRequest(@NotNull @Valid List<DataScopeItem> items) {
    }

    public record MenuUpsertRequest(
            Long parentId,
            @NotBlank @Size(max = 128) String menuCode,
            @NotBlank @Size(max = 128) String menuName,
            @Size(max = 255) String routePath,
            @Size(max = 255) String component,
            @Size(max = 64) String icon,
            Integer sortOrder,
            Integer visible,
            String status
    ) {
    }

    public record ButtonCreateRequest(
            @NotNull Long menuId,
            @NotBlank @Size(max = 128) String buttonCode,
            @NotBlank @Size(max = 128) String buttonName,
            @NotBlank @Size(max = 128) String permissionCode,
            String status
    ) {
    }

    public record TempAuthCreateRequest(
            @NotNull Long userId,
            @NotBlank @Size(max = 32) String authType,
            @NotNull Long targetId,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @NotBlank @Size(max = 500) String reason
    ) {
    }

    public record FieldPermCreateRequest(
            @NotNull Long roleId,
            @NotBlank @Size(max = 64) String businessType,
            @NotBlank @Size(max = 128) String fieldCode,
            Integer visible,
            Integer editable,
            Integer required,
            Integer masked
    ) {
    }

    public record FieldPermUpdateRequest(
            @NotNull Long roleId,
            @NotBlank @Size(max = 64) String businessType,
            @NotBlank @Size(max = 128) String fieldCode,
            Integer visible,
            Integer editable,
            Integer required,
            Integer masked
    ) {
    }
}
