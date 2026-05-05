package com.company.oa.permission;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permission")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:view')")
    @GetMapping("/roles")
    public PageResponse<Map<String, Object>> roles(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return permissionService.listRoles(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:view')")
    @GetMapping("/roles/{id}")
    public Map<String, Object> role(@PathVariable long id) {
        return permissionService.roleDetail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/roles")
    public Map<String, Object> createRole(@Valid @RequestBody PermissionDtos.RoleCreateRequest request) {
        return permissionService.createRole(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PutMapping("/roles/{id}")
    public Map<String, Object> updateRole(@PathVariable long id, @Valid @RequestBody PermissionDtos.RoleUpdateRequest request) {
        return permissionService.updateRole(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @DeleteMapping("/roles/{id}")
    public Map<String, Object> deleteRole(@PathVariable long id) {
        permissionService.deleteRole(id);
        return Map.of("deleted", true, "id", id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/roles/{id}/menus")
    public Map<String, Object> assignMenus(@PathVariable long id, @Valid @RequestBody PermissionDtos.AssignMenusRequest request) {
        return permissionService.assignMenus(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/roles/{id}/buttons")
    public Map<String, Object> assignButtons(@PathVariable long id, @Valid @RequestBody PermissionDtos.AssignButtonsRequest request) {
        return permissionService.assignButtons(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/roles/{id}/data-scopes")
    public Map<String, Object> assignDataScopes(@PathVariable long id, @Valid @RequestBody PermissionDtos.AssignDataScopesRequest request) {
        return permissionService.assignDataScopes(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:view')")
    @GetMapping("/menus/tree")
    public List<Map<String, Object>> menus() {
        return permissionService.menuTree();
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/menus")
    public Map<String, Object> createMenu(@Valid @RequestBody PermissionDtos.MenuUpsertRequest request) {
        return permissionService.createMenu(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PutMapping("/menus/{id}")
    public Map<String, Object> updateMenu(@PathVariable long id, @Valid @RequestBody PermissionDtos.MenuUpsertRequest request) {
        return permissionService.updateMenu(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:view')")
    @GetMapping("/buttons")
    public PageResponse<Map<String, Object>> buttons(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long menuId
    ) {
        return permissionService.listButtons(page, size, menuId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/buttons")
    public Map<String, Object> createButton(@Valid @RequestBody PermissionDtos.ButtonCreateRequest request) {
        return permissionService.createButton(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:view')")
    @GetMapping("/users/{userId}/preview")
    public Map<String, Object> preview(@PathVariable long userId) {
        return permissionService.previewUser(userId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:view')")
    @GetMapping("/field-permissions")
    public PageResponse<Map<String, Object>> fieldPermissions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String businessType
    ) {
        return permissionService.listFieldPermissions(page, size, roleId, businessType);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/field-permissions")
    public Map<String, Object> createFieldPermission(@Valid @RequestBody PermissionDtos.FieldPermCreateRequest request) {
        return permissionService.createFieldPermission(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PutMapping("/field-permissions/{id}")
    public Map<String, Object> updateFieldPermission(@PathVariable long id, @Valid @RequestBody PermissionDtos.FieldPermUpdateRequest request) {
        return permissionService.updateFieldPermission(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @DeleteMapping("/field-permissions/{id}")
    public Map<String, Object> deleteFieldPermission(@PathVariable long id) {
        permissionService.deleteFieldPermission(id);
        return Map.of("deleted", true, "id", id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:view')")
    @GetMapping("/temp-auths")
    public PageResponse<Map<String, Object>> tempAuths(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return permissionService.listTempAuths(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/temp-auths")
    public Map<String, Object> createTempAuth(@Valid @RequestBody PermissionDtos.TempAuthCreateRequest request) {
        return permissionService.createTempAuth(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PatchMapping("/temp-auths/{id}/revoke")
    public Map<String, Object> revokeTempAuth(@PathVariable long id) {
        return permissionService.revokeTempAuth(id);
    }
}
