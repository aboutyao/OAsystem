package com.company.oa.report;

import com.company.oa.BaseMySqlTest;
import com.company.oa.report.mapper.ReportSqlMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest extends BaseMySqlTest {

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(getMapper(ReportSqlMapper.class));
    }

    @Test
    void allSummaryQueriesRunWithoutError() {
        Map<String, Object> wf = service.workflowEfficiency(LocalDate.now().minusDays(7), LocalDate.now());
        assertThat(wf.get("totalInstances")).isNotNull();

        Map<String, Object> todo = service.todoSummary();
        assertThat(todo.get("pending")).isNotNull();

        Map<String, Object> leave = service.leaveSummary(null, null);
        assertThat(leave.get("totalCount")).isNotNull();

        Map<String, Object> expense = service.expenseSummary(null, null);
        assertThat(expense.get("totalCount")).isNotNull();

        Map<String, Object> contract = service.contractSummary(null, null);
        assertThat(contract.get("contractCount")).isNotNull();

        Map<String, Object> asset = service.assetSummary();
        assertThat(asset.get("assetCount")).isNotNull();

        Map<String, Object> user = service.userSummary();
        assertThat(user.get("totalUsers")).isNotNull();
    }
}