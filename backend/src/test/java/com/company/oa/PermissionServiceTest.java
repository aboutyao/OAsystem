package com.company.oa;

import com.company.oa.auth.AuthUser;
import com.company.oa.auth.AuthService;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.permission.PermissionDtos;
import com.company.oa.permission.PermissionService;
import com.company.oa.permission.cache.PermissionCacheService;
import com.company.oa.permission.mapper.*;
import com.company.oa.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceTest extends BaseMySqlTest {

    @Test
    void rolesMenusAndPreviewLoadFromDatabase() {
        AuthService authService = mock(AuthService.class);
        when(authService.currentUser()).thenReturn(new AuthUser(1L, "admin", "管理员", null, null, List.of("SUPER_ADMIN"), List.of("*")));

        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        PermissionCacheService cacheService = mock(PermissionCacheService.class);
        when(cacheService.getUserPreview(anyLong())).thenReturn(null);
        when(cacheService.getMenuTree()).thenReturn(null);
        PermissionService permissionService = new PermissionService(
                getMapper(PermRoleMapper.class),
                getMapper(PermMenuMapper.class),
                getMapper(PermButtonMapper.class),
                getMapper(PermUserRoleMapper.class),
                getMapper(PermRoleMenuMapper.class),
                getMapper(PermRoleButtonMapper.class),
                getMapper(PermDataScopeMapper.class),
                getMapper(PermDataScopeDeptMapper.class),
                getMapper(PermFieldPermissionMapper.class),
                getMapper(PermTempAuthMapper.class),
                getMapper(SysConfigMapper.class),
                authService,
                sequenceService,
                cacheService
        );

        assertThat(permissionService.listRoles(1, 20).total()).isGreaterThanOrEqualTo(3);
        assertThat(permissionService.menuTree()).isNotEmpty();

        Map<String, Object> preview = permissionService.previewUser(1L);
        @SuppressWarnings("unchecked")
        List<String> buttons = (List<String>) preview.get("buttons");
        assertThat(buttons).contains("*");

        permissionService.assignMenus(3L, new PermissionDtos.AssignMenusRequest(List.of(1L, 2L)));
        @SuppressWarnings("unchecked")
        List<Long> menuIds = (List<Long>) permissionService.roleDetail(3L).get("menuIds");
        assertThat(menuIds).hasSize(2);

        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        var temp = permissionService.createTempAuth(new PermissionDtos.TempAuthCreateRequest(
                1L, "MENU", 3L, start, end, "测试临时授权"
        ));
        assertThat(temp.get("status")).isEqualTo("ACTIVE");
    }
}
