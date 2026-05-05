package com.company.oa;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.rule.RuleDtos;
import com.company.oa.rule.RuleService;
import com.company.oa.rule.mapper.*;
import com.company.oa.system.mapper.SysConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleServiceTest extends BaseMySqlTest {

    private RuleService service;

    @BeforeEach
    void setUp() {
        AuthService auth = mock(AuthService.class);
        when(auth.currentUser()).thenReturn(
                new AuthUser(1L, "admin", "管理员", 2L, "总经办", List.of("SUPER_ADMIN"), List.of("*"))
        );
        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new RuleService(
                getMapper(RuleGroupMapper.class),
                getMapper(RuleDefinitionMapper.class),
                getMapper(RuleVersionMapper.class),
                getMapper(RuleAuditLogMapper.class),
                getMapper(SysConfigMapper.class),
                auth,
                objectMapper,
                sequenceService
        );
    }

    @Test
    void seededRulesAndSimulateAmount() {
        assertThat(service.listRules(1, 20).total()).isGreaterThanOrEqualTo(2);

        Map<String, Object> sim = service.simulate(1L, new RuleDtos.SimulateRequest("EXPENSE", Map.of("amount", 6000)));
        assertThat(sim.get("matched")).isEqualTo(true);

        Map<String, Object> simNo = service.simulate(1L, new RuleDtos.SimulateRequest("EXPENSE", Map.of("amount", 1000)));
        assertThat(simNo.get("matched")).isEqualTo(false);
    }
}