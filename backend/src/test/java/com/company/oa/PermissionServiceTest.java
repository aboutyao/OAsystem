package com.company.oa;

import com.company.oa.permission.PermissionDtos;
import com.company.oa.permission.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionServiceTest extends BaseSpringTest {

    @Autowired
    private PermissionService permissionService;

    @Test
    void rolesMenusAndPreviewLoadFromDatabase() {
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
