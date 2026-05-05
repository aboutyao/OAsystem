package com.company.oa;

import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.org.OrgDtos;
import com.company.oa.org.OrgService;
import com.company.oa.org.mapper.*;
import com.company.oa.permission.mapper.PermRoleMapper;
import com.company.oa.permission.mapper.PermUserRoleMapper;
import com.company.oa.system.SystemDtos;
import com.company.oa.system.SystemService;
import com.company.oa.system.cache.SystemCacheService;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.system.mapper.SysDictItemMapper;
import com.company.oa.system.mapper.SysDictTypeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrgServiceTest extends BaseMySqlTest {

    private OrgService orgService;
    private SystemService systemService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        orgService = new OrgService(
                getMapper(UserMapper.class),
                getMapper(DeptMapper.class),
                getMapper(PositionMapper.class),
                getMapper(RankMapper.class),
                getMapper(UserDeptMapper.class),
                getMapper(ChangeLogMapper.class),
                getMapper(PermUserRoleMapper.class),
                getMapper(PermRoleMapper.class),
                getMapper(SysConfigMapper.class),
                PasswordEncoderFactories.createDelegatingPasswordEncoder(),
                sequenceService,
                objectMapper
        );
        systemService = new SystemService(
                getMapper(SysConfigMapper.class),
                getMapper(SysDictTypeMapper.class),
                getMapper(SysDictItemMapper.class),
                sequenceService,
                mock(SystemCacheService.class)
        );
    }

    @Test
    void deptTreeAndUserCrudAgainstSeededDatabase() {
        assertThat(orgService.deptTree()).isNotEmpty();

        var page = orgService.listUsers(1, 20, null);
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
