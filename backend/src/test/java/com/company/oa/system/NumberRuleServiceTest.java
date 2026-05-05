package com.company.oa.system;

import com.company.oa.BaseMySqlTest;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.system.mapper.SysNumberRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumberRuleServiceTest extends BaseMySqlTest {

    private NumberRuleService service;

    @BeforeEach
    void setUp() {
        service = new NumberRuleService(getMapper(SysNumberRuleMapper.class),
                new SequenceService(getMapper(SysSequenceMapper.class)));
    }

    @Test
    void generatesUniqueIncrementingSequence() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String code = service.generateNext("EXPENSE_NO");
            assertThat(code).startsWith("EXP");
            assertThat(seen.add(code)).as("编号 " + code + " 不应重复").isTrue();
        }
    }

    @Test
    void rejectsDuplicateRuleCode() {
        jdbc.update("DELETE FROM sys_number_rule WHERE rule_code = 'TEST_NEW'");
        service.create(new SystemDtos.NumberRuleCreateRequest(
                "TEST_NEW", "GENERIC", "T", null, 4, "DAILY", null
        ));
        assertThatThrownBy(() -> service.create(new SystemDtos.NumberRuleCreateRequest(
                "TEST_NEW", "GENERIC", "T", null, 4, "DAILY", null
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsInvalidReset() {
        assertThatThrownBy(() -> service.create(new SystemDtos.NumberRuleCreateRequest(
                "BAD_RULE", "GENERIC", "B", null, 4, "WEEKLY", null
        ))).isInstanceOf(BusinessException.class);
    }
}
