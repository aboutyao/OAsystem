package com.company.oa;

import com.company.oa.rule.RuleDtos;
import com.company.oa.rule.RuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleServiceTest extends BaseSpringTest {

    @Autowired
    private RuleService service;

    @Test
    void seededRulesAndSimulateAmount() {
        assertThat(service.listRules(1, 20).total()).isGreaterThanOrEqualTo(2);

        Map<String, Object> sim = service.simulate(1L, new RuleDtos.SimulateRequest("EXPENSE", Map.of("amount", 6000)));
        assertThat(sim.get("matched")).isEqualTo(true);

        Map<String, Object> simNo = service.simulate(1L, new RuleDtos.SimulateRequest("EXPENSE", Map.of("amount", 1000)));
        assertThat(simNo.get("matched")).isEqualTo(false);
    }

    @Test
    void listRulesPagination() {
        var page1 = service.listRules(1, 5);
        assertThat(page1.items()).isNotNull();
        assertThat(page1.page()).isEqualTo(1);
        assertThat(page1.size()).isEqualTo(5);
    }

    @Test
    void simulateWithNullValueReturnsDefaultMatch() {
        Map<String, Object> sim = service.simulate(1L, new RuleDtos.SimulateRequest("EXPENSE", Map.of()));
        // Should not throw, returns a result
        assertThat(sim).containsKey("matched");
    }

    @Test
    void simulateDifferentBusinessTypes() {
        Map<String, Object> sim = service.simulate(1L, new RuleDtos.SimulateRequest("LEAVE", Map.of("days", 5)));
        assertThat(sim).containsKey("matched");
    }
}
