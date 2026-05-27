package com.company.oa;

import com.company.oa.org.OrgDtos;
import com.company.oa.org.OrgService;
import com.company.oa.system.SystemDtos;
import com.company.oa.system.SystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrgServiceTest extends BaseSpringTest {

    @Autowired
    private OrgService orgService;

    @Autowired
    private SystemService systemService;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM org_user WHERE username = 'zhangsan'");
    }

    @Test
    void deptTreeAndUserCrudAgainstSeededDatabase() {
        assertThat(orgService.deptTree()).isNotEmpty();

        var page = orgService.listUsers(1, 20, null, null, null, null);
        assertThat(page.total()).isGreaterThanOrEqualTo(1);

        var created = orgService.createUser(new OrgDtos.UserCreateRequest(
                "zhangsan", "E9999", "张三", null, null, 3L, 2L, 2L, null, null, List.of(3L)
        ));
        assertThat(created.get("username")).isEqualTo("zhangsan");

        long id = ((Number) created.get("id")).longValue();
        assertThat(orgService.userDetail(id).get("realName")).isEqualTo("张三");

        orgService.updateUser(id, new OrgDtos.UserUpdateRequest(
                "E9999", "张三三", null, null, 3L, 2L, 2L, null, List.of(3L)
        ));
        assertThat(orgService.userDetail(id).get("realName")).isEqualTo("张三三");

        var cfg = systemService.updateConfig("security.login.maxFailCount", new SystemDtos.ConfigUpdateRequest("7"));
        assertThat(cfg.get("configValue")).isEqualTo("7");
    }
}
