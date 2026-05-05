package com.company.oa.contract;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long ownerId
    ) {
        return contractService.list(page, size, ownerId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        return contractService.detail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody ContractDtos.ContractCreateRequest request) {
        return contractService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody ContractDtos.ContractUpdateRequest request) {
        return contractService.update(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/submit")
    public Map<String, Object> submit(@PathVariable long id) {
        return contractService.submit(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/sign")
    public Map<String, Object> sign(@PathVariable long id) {
        return contractService.signContract(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/terminate")
    public Map<String, Object> terminate(@PathVariable long id) {
        return contractService.terminateContract(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/renew")
    public Map<String, Object> renew(@PathVariable long id) {
        return contractService.renewContract(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}/versions")
    public List<Map<String, Object>> versions(@PathVariable long id) {
        return contractService.versions(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/export")
    public Map<String, Object> export(@RequestBody(required = false) Map<String, Object> filter) {
        return Map.of(
                "message", "导出任务尚未接入",
                "filter", filter == null ? Map.of() : filter
        );
    }
}
