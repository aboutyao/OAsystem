package com.company.oa.contract;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractRiskController {
    private final ContractRiskService riskService;

    public ContractRiskController(ContractRiskService riskService) {
        this.riskService = riskService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'contract:view')")
    @GetMapping("/{id}/risks")
    public List<Map<String, Object>> getRisks(@PathVariable long id) {
        return riskService.checkContractRisks(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'contract:view')")
    @GetMapping("/risks/report")
    public Map<String, Object> getRiskReport() {
        return riskService.getContractRiskReport();
    }
}
