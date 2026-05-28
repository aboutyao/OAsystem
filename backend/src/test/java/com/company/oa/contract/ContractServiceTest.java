package com.company.oa.contract;

import com.company.oa.BaseSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractServiceTest extends BaseSpringTest {

    @Autowired
    private ContractService contractService;

    @Test
    void createAndDetail() {
        var req = new ContractDtos.ContractCreateRequest(
                "测试合同", "PURCHASE", "供应商A", BigDecimal.valueOf(50000),
                LocalDate.now(), LocalDate.now().plusMonths(12)
        );
        var created = contractService.create(req);
        assertThat(created.get("id")).isNotNull();
        assertThat(created.get("contractName")).isEqualTo("测试合同");
        assertThat(created.get("status")).isEqualTo("DRAFT");

        var detail = contractService.detail(((Number) created.get("id")).longValue());
        assertThat(detail.get("contractNo")).isNotNull();
    }

    @Test
    void updateDraftContract() {
        var req = new ContractDtos.ContractCreateRequest(
                "原合同名", "SERVICE", "客户B", BigDecimal.valueOf(10000),
                LocalDate.now(), LocalDate.now().plusMonths(6)
        );
        var created = contractService.create(req);
        long id = ((Number) created.get("id")).longValue();

        var updateReq = new ContractDtos.ContractUpdateRequest(
                "更新后合同名", "SERVICE", "客户B", BigDecimal.valueOf(15000),
                LocalDate.now(), LocalDate.now().plusMonths(9)
        );
        var updated = contractService.update(id, updateReq);
        assertThat(updated.get("contractName")).isEqualTo("更新后合同名");
    }

    @Test
    void listContracts() {
        var req = new ContractDtos.ContractCreateRequest(
                "列表测试合同", "PURCHASE", "供应商C", BigDecimal.valueOf(30000),
                LocalDate.now(), LocalDate.now().plusMonths(6)
        );
        contractService.create(req);
        var page = contractService.list(1, 20, null);
        assertThat(page.total()).isGreaterThanOrEqualTo(1);
        assertThat(page.items()).isNotEmpty();
    }

    @Test
    void cancelDraftContract() {
        var req = new ContractDtos.ContractCreateRequest(
                "作废测试合同", "PURCHASE", "供应商D", BigDecimal.valueOf(20000),
                LocalDate.now(), LocalDate.now().plusMonths(3)
        );
        var created = contractService.create(req);
        long id = ((Number) created.get("id")).longValue();

        var cancelled = contractService.cancel(id);
        assertThat(cancelled.get("status")).isEqualTo("CANCELLED");
    }

    @Test
    void terminateApprovedContract() {
        var req = new ContractDtos.ContractCreateRequest(
                "终止测试合同", "SERVICE", "客户E", BigDecimal.valueOf(80000),
                LocalDate.now(), LocalDate.now().plusMonths(12)
        );
        var created = contractService.create(req);
        long id = ((Number) created.get("id")).longValue();

        // Directly set status to APPROVED for testing
        jdbc.update("UPDATE contract_info SET status = 'APPROVED' WHERE id = ?", id);

        var terminated = contractService.terminateContract(id);
        assertThat(terminated.get("status")).isEqualTo("TERMINATED");
    }
}
