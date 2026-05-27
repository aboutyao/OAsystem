package com.company.oa.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.oa.auth.AuthService;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.perm.*;
import com.company.oa.permission.mapper.*;
import com.company.oa.permission.cache.PermissionCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {
    private static final String ENABLED = "ENABLED";
    private static final String SYSTEM = "SYSTEM";

    private final PermRoleMapper permRoleMapper;
    private final PermMenuMapper permMenuMapper;
    private final PermButtonMapper permButtonMapper;
    private final PermUserRoleMapper permUserRoleMapper;
    private final PermRoleMenuMapper permRoleMenuMapper;
    private final PermRoleButtonMapper permRoleButtonMapper;
    private final PermDataScopeMapper permDataScopeMapper;
    private final PermDataScopeDeptMapper permDataScopeDeptMapper;
    private final PermFieldPermissionMapper permFieldPermissionMapper;
    private final PermTempAuthMapper permTempAuthMapper;
    private final PaginationHelper paginationHelper;
    private final AuthService authService;
    private final SequenceService sequenceService;
    private final PermissionCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public PermissionService(PermRoleMapper permRoleMapper,
                             PermMenuMapper permMenuMapper,
                             PermButtonMapper permButtonMapper,
                             PermUserRoleMapper permUserRoleMapper,
                             PermRoleMenuMapper permRoleMenuMapper,
                             PermRoleButtonMapper permRoleButtonMapper,
                             PermDataScopeMapper permDataScopeMapper,
                             PermDataScopeDeptMapper permDataScopeDeptMapper,
                             PermFieldPermissionMapper permFieldPermissionMapper,
                             PermTempAuthMapper permTempAuthMapper,
                             PaginationHelper paginationHelper,
                             AuthService authService,
                             SequenceService sequenceService,
                             PermissionCacheService cacheService) {
        this.permRoleMapper = permRoleMapper;
        this.permMenuMapper = permMenuMapper;
        this.permButtonMapper = permButtonMapper;
        this.permUserRoleMapper = permUserRoleMapper;
        this.permRoleMenuMapper = permRoleMenuMapper;
        this.permRoleButtonMapper = permRoleButtonMapper;
        this.permDataScopeMapper = permDataScopeMapper;
        this.permDataScopeDeptMapper = permDataScopeDeptMapper;
        this.permFieldPermissionMapper = permFieldPermissionMapper;
        this.permTempAuthMapper = permTempAuthMapper;
        this.paginationHelper = paginationHelper;
        this.authService = authService;
        this.sequenceService = sequenceService;
        this.cacheService = cacheService;
    }

    // ===================== helpers =====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, (Class<Map<String, Object>>) (Class) Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(List<?> entities) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object e : entities) {
            result.add((Map<String, Object>) objectMapper.convertValue(e, Map.class));
        }
        return result;
    }

    // ===================== roles =====================

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listRoles(long page, long size) {
        long[] ps = paginationHelper.clamp(page, size);
        long total = permRoleMapper.selectCount(new LambdaQueryWrapper<PermRole>());
        LambdaQueryWrapper<PermRole> wrapper = new LambdaQueryWrapper<PermRole>()
                .orderByAsc(PermRole::getSortOrder)
                .orderByAsc(PermRole::getId);
        Page<PermRole> pageResult = permRoleMapper.selectPage(new Page<>(ps[0], ps[1]), wrapper);
        List<Map<String, Object>> items = toMapList(pageResult.getRecords());
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> roleDetail(long id) {
        Map<String, Object> role = loadRole(id);
        List<Long> menuIds = permRoleMenuMapper.selectMenuIdsByRoleId(id);
        List<Long> buttonIds = permRoleButtonMapper.selectButtonIdsByRoleId(id);
        List<Map<String, Object>> dataScopes = listDataScopesForRole(id);
        Map<String, Object> out = new LinkedHashMap<>(role);
        out.put("menuIds", menuIds);
        out.put("buttonIds", buttonIds);
        out.put("dataScopes", dataScopes);
        return out;
    }

    @Transactional
    public Map<String, Object> createRole(PermissionDtos.RoleCreateRequest req) {
        if (roleCodeTaken(req.roleCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        long id = sequenceService.nextId("perm_role");
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        int sort = req.sortOrder() == null ? 0 : req.sortOrder();
        LocalDateTime now = LocalDateTime.now();
        PermRole entity = new PermRole();
        entity.setId(id);
        entity.setRoleCode(req.roleCode());
        entity.setRoleName(req.roleName());
        entity.setRoleType(req.roleType());
        entity.setStatus(status);
        entity.setSortOrder(sort);
        entity.setRemark(req.remark());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        permRoleMapper.insert(entity);
        return roleDetail(id);
    }

    @Transactional
    public Map<String, Object> updateRole(long id, PermissionDtos.RoleUpdateRequest req) {
        Map<String, Object> before = loadRole(id);
        String beforeCode = String.valueOf(before.get("roleCode"));
        if ("SUPER_ADMIN".equals(beforeCode) && !"SUPER_ADMIN".equals(req.roleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改超级管理员角色编码");
        }
        if (roleCodeTaken(req.roleCode(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        int sort = req.sortOrder() == null
                ? ((Number) before.getOrDefault("sortOrder", 0)).intValue()
                : req.sortOrder();
        String status = StringUtils.hasText(req.status())
                ? req.status()
                : String.valueOf(before.get("status"));
        PermRole entity = new PermRole();
        entity.setId(id);
        entity.setRoleCode(req.roleCode());
        entity.setRoleName(req.roleName());
        entity.setRoleType(req.roleType());
        entity.setStatus(status);
        entity.setSortOrder(sort);
        entity.setRemark(req.remark());
        entity.setUpdatedAt(now);
        permRoleMapper.updateById(entity);
        return roleDetail(id);
    }

    @Transactional
    public void deleteRole(long id) {
        Map<String, Object> role = loadRole(id);
        if ("SUPER_ADMIN".equals(String.valueOf(role.get("roleCode")))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能删除超级管理员角色");
        }
        if (SYSTEM.equals(String.valueOf(role.get("roleType")))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能删除系统内置角色");
        }
        Long userCount = permUserRoleMapper.selectCount(
                new LambdaQueryWrapper<PermUserRole>().eq(PermUserRole::getRoleId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色仍分配给用户，无法删除");
        }
        permRoleButtonMapper.delete(
                new LambdaQueryWrapper<PermRoleButton>().eq(PermRoleButton::getRoleId, id));
        permRoleMenuMapper.delete(
                new LambdaQueryWrapper<PermRoleMenu>().eq(PermRoleMenu::getRoleId, id));
        List<Long> scopeIds = permDataScopeMapper.selectList(
                new LambdaQueryWrapper<PermDataScope>().eq(PermDataScope::getRoleId, id)
                        .select(PermDataScope::getId))
                .stream().map(PermDataScope::getId).collect(Collectors.toList());
        if (!scopeIds.isEmpty()) {
            permDataScopeDeptMapper.delete(
                    new LambdaQueryWrapper<PermDataScopeDept>()
                            .in(PermDataScopeDept::getDataScopeId, scopeIds));
        }
        permDataScopeMapper.delete(
                new LambdaQueryWrapper<PermDataScope>().eq(PermDataScope::getRoleId, id));
        permFieldPermissionMapper.delete(
                new LambdaQueryWrapper<PermFieldPermission>()
                        .eq(PermFieldPermission::getRoleId, id));
        permRoleMapper.deleteById(id);
        cacheService.invalidateAll();
    }

    // ===================== menus =====================

    @Transactional
    public Map<String, Object> assignMenus(long roleId, PermissionDtos.AssignMenusRequest req) {
        loadRole(roleId);
        for (Long menuId : req.menuIds()) {
            if (!menuExists(menuId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "菜单不存在: " + menuId);
            }
        }
        permRoleMenuMapper.delete(
                new LambdaQueryWrapper<PermRoleMenu>().eq(PermRoleMenu::getRoleId, roleId));
        for (Long menuId : req.menuIds()) {
            long rid = sequenceService.nextId("perm_role_menu");
            PermRoleMenu rm = new PermRoleMenu();
            rm.setId(rid);
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            permRoleMenuMapper.insert(rm);
        }
        cacheService.invalidateAll();
        return Map.of("roleId", roleId, "menuCount", req.menuIds().size());
    }

    @Transactional
    public Map<String, Object> assignButtons(long roleId, PermissionDtos.AssignButtonsRequest req) {
        loadRole(roleId);
        for (Long buttonId : req.buttonIds()) {
            if (!buttonExists(buttonId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "按钮不存在: " + buttonId);
            }
        }
        permRoleButtonMapper.delete(
                new LambdaQueryWrapper<PermRoleButton>().eq(PermRoleButton::getRoleId, roleId));
        for (Long buttonId : req.buttonIds()) {
            long rid = sequenceService.nextId("perm_role_button");
            PermRoleButton rb = new PermRoleButton();
            rb.setId(rid);
            rb.setRoleId(roleId);
            rb.setButtonId(buttonId);
            permRoleButtonMapper.insert(rb);
        }
        cacheService.invalidateAll();
        return Map.of("roleId", roleId, "buttonCount", req.buttonIds().size());
    }

    @Transactional
    public Map<String, Object> assignDataScopes(long roleId, PermissionDtos.AssignDataScopesRequest req) {
        loadRole(roleId);
        List<Long> oldScopeIds = permDataScopeMapper.selectList(
                new LambdaQueryWrapper<PermDataScope>().eq(PermDataScope::getRoleId, roleId)
                        .select(PermDataScope::getId))
                .stream().map(PermDataScope::getId).collect(Collectors.toList());
        if (!oldScopeIds.isEmpty()) {
            permDataScopeDeptMapper.delete(
                    new LambdaQueryWrapper<PermDataScopeDept>()
                            .in(PermDataScopeDept::getDataScopeId, oldScopeIds));
        }
        permDataScopeMapper.delete(
                new LambdaQueryWrapper<PermDataScope>().eq(PermDataScope::getRoleId, roleId));
        LocalDateTime now = LocalDateTime.now();
        for (PermissionDtos.DataScopeItem item : req.items()) {
            long scopeId = sequenceService.nextId("perm_data_scope");
            PermDataScope scope = new PermDataScope();
            scope.setId(scopeId);
            scope.setRoleId(roleId);
            scope.setScopeType(item.scopeType());
            scope.setBusinessType(item.businessType());
            scope.setCreatedAt(now);
            permDataScopeMapper.insert(scope);
            List<Long> deptIds = item.deptIds() == null ? List.of() : item.deptIds();
            for (Long deptId : deptIds) {
                long did = sequenceService.nextId("perm_data_scope_dept");
                PermDataScopeDept dsd = new PermDataScopeDept();
                dsd.setId(did);
                dsd.setDataScopeId(scopeId);
                dsd.setDeptId(deptId);
                permDataScopeDeptMapper.insert(dsd);
            }
        }
        cacheService.invalidateAll();
        return Map.of("roleId", roleId, "scopeCount", req.items().size());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> menuTree() {
        List<Map<String, Object>> cached = cacheService.getMenuTree();
        if (cached != null) {
            return cached;
        }
        List<PermMenu> menus = permMenuMapper.selectList(
                new LambdaQueryWrapper<PermMenu>()
                        .orderByAsc(PermMenu::getSortOrder)
                        .orderByAsc(PermMenu::getId));
        List<Map<String, Object>> rows = toMapList(menus);
        List<Map<String, Object>> tree = buildMenuTree(rows);
        cacheService.setMenuTree(tree);
        return tree;
    }

    @Transactional
    public Map<String, Object> createMenu(PermissionDtos.MenuUpsertRequest req) {
        if (menuCodeTaken(req.menuCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "菜单编码已存在");
        }
        if (req.parentId() != null && !menuExists(req.parentId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上级菜单不存在");
        }
        long id = sequenceService.nextId("perm_menu");
        int sort = req.sortOrder() == null ? 0 : req.sortOrder();
        int vis = req.visible() == null ? 1 : req.visible();
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        PermMenu entity = new PermMenu();
        entity.setId(id);
        entity.setParentId(req.parentId());
        entity.setMenuCode(req.menuCode());
        entity.setMenuName(req.menuName());
        entity.setRoutePath(req.routePath());
        entity.setComponent(req.component());
        entity.setIcon(req.icon());
        entity.setSortOrder(sort);
        entity.setVisible((short) vis);
        entity.setStatus(status);
        permMenuMapper.insert(entity);
        cacheService.invalidateMenuTree();
        return menuRow(id);
    }

    @Transactional
    public Map<String, Object> updateMenu(long id, PermissionDtos.MenuUpsertRequest req) {
        menuRow(id);
        if (req.parentId() != null) {
            if (req.parentId() == id) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能将菜单设为自己的上级");
            }
            if (!menuExists(req.parentId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "上级菜单不存在");
            }
            if (isMenuDescendant(id, req.parentId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能将上级设为当前菜单的下级");
            }
        }
        if (menuCodeTaken(req.menuCode(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "菜单编码已存在");
        }
        int sort = req.sortOrder() == null ? 0 : req.sortOrder();
        int vis = req.visible() == null ? 1 : req.visible();
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        PermMenu entity = new PermMenu();
        entity.setId(id);
        entity.setParentId(req.parentId());
        entity.setMenuCode(req.menuCode());
        entity.setMenuName(req.menuName());
        entity.setRoutePath(req.routePath());
        entity.setComponent(req.component());
        entity.setIcon(req.icon());
        entity.setSortOrder(sort);
        entity.setVisible((short) vis);
        entity.setStatus(status);
        permMenuMapper.updateById(entity);
        cacheService.invalidateMenuTree();
        return menuRow(id);
    }

    // ===================== buttons =====================

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listButtons(long page, long size, Long menuId) {
        long[] ps = paginationHelper.clamp(page, size);
        LambdaQueryWrapper<PermButton> wrapper = new LambdaQueryWrapper<PermButton>()
                .eq(menuId != null, PermButton::getMenuId, menuId)
                .orderByAsc(PermButton::getMenuId)
                .orderByAsc(PermButton::getId);
        long total = permButtonMapper.selectCount(wrapper);
        Page<PermButton> pageResult = permButtonMapper.selectPage(new Page<>(ps[0], ps[1]), wrapper);
        List<Map<String, Object>> items = toMapList(pageResult.getRecords());
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> createButton(PermissionDtos.ButtonCreateRequest req) {
        if (!menuExists(req.menuId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "所属菜单不存在");
        }
        if (permissionCodeTaken(req.permissionCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "权限标识已存在");
        }
        long id = sequenceService.nextId("perm_button");
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        PermButton entity = new PermButton();
        entity.setId(id);
        entity.setMenuId(req.menuId());
        entity.setButtonCode(req.buttonCode());
        entity.setButtonName(req.buttonName());
        entity.setPermissionCode(req.permissionCode());
        entity.setStatus(status);
        permButtonMapper.insert(entity);
        return buttonRow(id);
    }

    // ===================== previewUser =====================

    @Transactional(readOnly = true)
    public Map<String, Object> previewUser(long userId) {
        Map<String, Object> cached = cacheService.getUserPreview(userId);
        if (cached != null) {
            return cached;
        }
        if (!userExists(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        List<String> roleCodes = permUserRoleMapper.selectRoleCodesByUserId(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", userId);
        if (roleCodes.contains("SUPER_ADMIN")) {
            out.put("menus", menuTree());
            out.put("buttons", List.of("*"));
            out.put("dataScopes", List.of(Map.of("scopeType", "ALL", "businessType", "*", "deptIds", List.of())));
            out.put("fieldPermissions", List.of());
            cacheService.setUserPreview(userId, out);
            return out;
        }
        Set<Long> menuIds = new HashSet<>(permUserRoleMapper.selectMenuIdsByUserId(userId));
        expandMenuAncestors(menuIds);
        List<Map<String, Object>> menuRows = menuRowsByIds(menuIds);
        out.put("menus", buildMenuTree(menuRows));

        List<String> buttons = permUserRoleMapper.selectButtonPermissionCodesByUserId(userId);
        out.put("buttons", buttons);

        List<Map<String, Object>> scopes = permUserRoleMapper.selectDataScopesByUserId(userId);
        for (Map<String, Object> s : scopes) {
            long sid = ((Number) s.get("id")).longValue();
            List<Long> deptIds = permDataScopeDeptMapper.selectList(
                    new LambdaQueryWrapper<PermDataScopeDept>()
                            .eq(PermDataScopeDept::getDataScopeId, sid)
                            .orderByAsc(PermDataScopeDept::getDeptId)
                            .select(PermDataScopeDept::getDeptId))
                    .stream().map(PermDataScopeDept::getDeptId).collect(Collectors.toList());
            s.put("deptIds", deptIds);
            s.remove("id");
        }
        out.put("dataScopes", scopes);

        List<Map<String, Object>> fields = permUserRoleMapper.selectFieldPermissionsByUserId(userId);
        out.put("fieldPermissions", fields);
        cacheService.setUserPreview(userId, out);
        return out;
    }

    // ===================== temp auth =====================

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listTempAuths(long page, long size) {
        long[] ps = paginationHelper.clamp(page, size);
        long total = permTempAuthMapper.selectCount(new LambdaQueryWrapper<PermTempAuth>());
        LambdaQueryWrapper<PermTempAuth> wrapper = new LambdaQueryWrapper<PermTempAuth>()
                .orderByDesc(PermTempAuth::getId);
        Page<PermTempAuth> pageResult = permTempAuthMapper.selectPage(new Page<>(ps[0], ps[1]), wrapper);
        List<Map<String, Object>> items = toMapList(pageResult.getRecords());
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> createTempAuth(PermissionDtos.TempAuthCreateRequest req) {
        if (!userExists(req.userId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        if (!req.endAt().isAfter(req.startAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        long id = sequenceService.nextId("perm_temp_auth");
        LocalDateTime start = LocalDateTime.ofInstant(req.startAt(), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(req.endAt(), ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        long createdBy = authService.currentUser().id();
        PermTempAuth entity = new PermTempAuth();
        entity.setId(id);
        entity.setUserId(req.userId());
        entity.setAuthType(req.authType());
        entity.setTargetId(req.targetId());
        entity.setStartAt(start);
        entity.setEndAt(end);
        entity.setReason(req.reason());
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(now);
        permTempAuthMapper.insert(entity);
        return tempAuthRow(id);
    }

    @Transactional
    public Map<String, Object> revokeTempAuth(long id) {
        tempAuthRow(id);
        PermTempAuth entity = new PermTempAuth();
        entity.setId(id);
        entity.setStatus("REVOKED");
        permTempAuthMapper.updateById(entity);
        return tempAuthRow(id);
    }

    // ===================== field permissions =====================

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listFieldPermissions(long page, long size,
                                                                   Long roleId, String businessType) {
        long[] ps = paginationHelper.clamp(page, size);
        LambdaQueryWrapper<PermFieldPermission> wrapper = new LambdaQueryWrapper<PermFieldPermission>()
                .eq(roleId != null, PermFieldPermission::getRoleId, roleId)
                .eq(StringUtils.hasText(businessType), PermFieldPermission::getBusinessType,
                        StringUtils.hasText(businessType) ? businessType.trim() : null)
                .orderByAsc(PermFieldPermission::getRoleId)
                .orderByAsc(PermFieldPermission::getBusinessType)
                .orderByAsc(PermFieldPermission::getFieldCode);
        long total = permFieldPermissionMapper.selectCount(wrapper);
        Page<PermFieldPermission> pageResult = permFieldPermissionMapper.selectPage(
                new Page<>(ps[0], ps[1]), wrapper);
        // Enrich with role names
        Map<Long, String> roleNameCache = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (PermFieldPermission fp : pageResult.getRecords()) {
            Map<String, Object> m = toMap(fp);
            Long rid = fp.getRoleId();
            if (rid != null) {
                roleNameCache.computeIfAbsent(rid, k -> {
                    PermRole r = permRoleMapper.selectById(k);
                    return r != null ? r.getRoleName() : null;
                });
                m.put("roleName", roleNameCache.get(rid));
            }
            items.add(m);
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> createFieldPermission(PermissionDtos.FieldPermCreateRequest req) {
        if (!roleExists(req.roleId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在");
        }
        Long dup = permFieldPermissionMapper.selectCount(
                new LambdaQueryWrapper<PermFieldPermission>()
                        .eq(PermFieldPermission::getRoleId, req.roleId())
                        .eq(PermFieldPermission::getBusinessType, req.businessType())
                        .eq(PermFieldPermission::getFieldCode, req.fieldCode()));
        if (dup != null && dup > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "字段权限配置已存在");
        }
        long id = sequenceService.nextId("perm_field_permission");
        int vis = req.visible() == null ? 1 : req.visible();
        int edit = req.editable() == null ? 0 : req.editable();
        int reqd = req.required() == null ? 0 : req.required();
        int mask = req.masked() == null ? 0 : req.masked();
        PermFieldPermission entity = new PermFieldPermission();
        entity.setId(id);
        entity.setRoleId(req.roleId());
        entity.setBusinessType(req.businessType());
        entity.setFieldCode(req.fieldCode());
        entity.setVisible((short) vis);
        entity.setEditable((short) edit);
        entity.setRequired((short) reqd);
        entity.setMasked((short) mask);
        permFieldPermissionMapper.insert(entity);
        return fieldPermRow(id);
    }

    @Transactional
    public Map<String, Object> updateFieldPermission(long id, PermissionDtos.FieldPermUpdateRequest req) {
        fieldPermRow(id);
        int vis = req.visible() == null ? 1 : req.visible();
        int edit = req.editable() == null ? 0 : req.editable();
        int reqd = req.required() == null ? 0 : req.required();
        int mask = req.masked() == null ? 0 : req.masked();
        PermFieldPermission entity = new PermFieldPermission();
        entity.setId(id);
        entity.setRoleId(req.roleId());
        entity.setBusinessType(req.businessType());
        entity.setFieldCode(req.fieldCode());
        entity.setVisible((short) vis);
        entity.setEditable((short) edit);
        entity.setRequired((short) reqd);
        entity.setMasked((short) mask);
        permFieldPermissionMapper.updateById(entity);
        return fieldPermRow(id);
    }

    @Transactional
    public void deleteFieldPermission(long id) {
        fieldPermRow(id);
        permFieldPermissionMapper.deleteById(id);
    }

    // ===================== private helpers =====================

    private Map<String, Object> fieldPermRow(long id) {
        Map<String, Object> row = permFieldPermissionMapper.selectWithRoleNameById(id);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字段权限不存在");
        }
        return row;
    }

    private boolean roleExists(long id) {
        Long n = permRoleMapper.selectCount(
                new LambdaQueryWrapper<PermRole>().eq(PermRole::getId, id));
        return n != null && n > 0;
    }

    private Map<String, Object> tempAuthRow(long id) {
        PermTempAuth entity = permTempAuthMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "临时授权不存在");
        }
        return toMap(entity);
    }

    private List<Map<String, Object>> listDataScopesForRole(long roleId) {
        List<PermDataScope> scopes = permDataScopeMapper.selectList(
                new LambdaQueryWrapper<PermDataScope>()
                        .eq(PermDataScope::getRoleId, roleId));
        List<Map<String, Object>> result = toMapList(scopes);
        for (Map<String, Object> s : result) {
            long sid = ((Number) s.get("id")).longValue();
            List<Long> deptIds = permDataScopeDeptMapper.selectList(
                    new LambdaQueryWrapper<PermDataScopeDept>()
                            .eq(PermDataScopeDept::getDataScopeId, sid)
                            .orderByAsc(PermDataScopeDept::getDeptId)
                            .select(PermDataScopeDept::getDeptId))
                    .stream().map(PermDataScopeDept::getDeptId).collect(Collectors.toList());
            s.put("deptIds", deptIds);
            s.remove("id");
        }
        return result;
    }

    private Map<String, Object> loadRole(long id) {
        PermRole entity = permRoleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return new LinkedHashMap<>(toMap(entity));
    }

    private Map<String, Object> menuRow(long id) {
        PermMenu entity = permMenuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        return toMap(entity);
    }

    private Map<String, Object> buttonRow(long id) {
        PermButton entity = permButtonMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "按钮不存在");
        }
        return toMap(entity);
    }

    private boolean menuExists(long id) {
        Long n = permMenuMapper.selectCount(
                new LambdaQueryWrapper<PermMenu>().eq(PermMenu::getId, id));
        return n != null && n > 0;
    }

    private boolean buttonExists(long id) {
        Long n = permButtonMapper.selectCount(
                new LambdaQueryWrapper<PermButton>().eq(PermButton::getId, id));
        return n != null && n > 0;
    }

    private boolean userExists(long id) {
        return permUserRoleMapper.selectCount(
                new LambdaQueryWrapper<PermUserRole>().eq(PermUserRole::getUserId, id)) > 0;
    }

    private boolean roleCodeTaken(String code, Long excludeId) {
        LambdaQueryWrapper<PermRole> wrapper = new LambdaQueryWrapper<PermRole>()
                .eq(PermRole::getRoleCode, code);
        if (excludeId != null) {
            wrapper.ne(PermRole::getId, excludeId);
        }
        Long n = permRoleMapper.selectCount(wrapper);
        return n != null && n > 0;
    }

    private boolean menuCodeTaken(String code, Long excludeId) {
        LambdaQueryWrapper<PermMenu> wrapper = new LambdaQueryWrapper<PermMenu>()
                .eq(PermMenu::getMenuCode, code);
        if (excludeId != null) {
            wrapper.ne(PermMenu::getId, excludeId);
        }
        Long n = permMenuMapper.selectCount(wrapper);
        return n != null && n > 0;
    }

    private boolean permissionCodeTaken(String code, Long excludeId) {
        LambdaQueryWrapper<PermButton> wrapper = new LambdaQueryWrapper<PermButton>()
                .eq(PermButton::getPermissionCode, code);
        if (excludeId != null) {
            wrapper.ne(PermButton::getId, excludeId);
        }
        Long n = permButtonMapper.selectCount(wrapper);
        return n != null && n > 0;
    }

    private void expandMenuAncestors(Set<Long> menuIds) {
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < 100) {
            changed = false;
            for (Long mid : new HashSet<>(menuIds)) {
                PermMenu menu = permMenuMapper.selectById(mid);
                if (menu != null && menu.getParentId() != null) {
                    if (menuIds.add(menu.getParentId())) {
                        changed = true;
                    }
                }
            }
        }
    }

    private List<Map<String, Object>> menuRowsByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<PermMenu> menus = permMenuMapper.selectList(
                new LambdaQueryWrapper<PermMenu>().in(PermMenu::getId, ids));
        return menus.stream()
                .sorted(Comparator.comparingInt(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildMenuTree(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(id, node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            Map<String, Object> node = byId.get(id);
            Object pid = row.get("parentId");
            if (pid == null) {
                roots.add(node);
            } else {
                long parentId = ((Number) pid).longValue();
                Map<String, Object> parent = byId.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> ch = (List<Map<String, Object>>) parent.get("children");
                    ch.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        sortMenuChildren(roots);
        return roots;
    }

    private void sortMenuChildren(List<Map<String, Object>> nodes) {
        nodes.sort(Comparator.comparingInt(n -> ((Number) n.getOrDefault("sortOrder", 0)).intValue()));
        for (Map<String, Object> n : nodes) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ch = (List<Map<String, Object>>) n.get("children");
            if (ch != null && !ch.isEmpty()) {
                sortMenuChildren(ch);
            }
        }
    }

    private boolean isMenuDescendant(long ancestorId, long possibleDescendantId) {
        if (ancestorId == possibleDescendantId) {
            return true;
        }
        List<PermMenu> allMenus = permMenuMapper.selectList(null);
        Map<Long, Long> parentMap = new LinkedHashMap<>();
        for (PermMenu m : allMenus) {
            parentMap.put(m.getId(), m.getParentId());
        }
        Long cur = possibleDescendantId;
        int guard = 0;
        while (cur != null && guard++ < 1000) {
            if (cur == ancestorId) {
                return true;
            }
            cur = parentMap.get(cur);
        }
        return false;
    }

}
