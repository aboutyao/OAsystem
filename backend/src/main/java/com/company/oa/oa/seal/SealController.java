package com.company.oa.oa.seal;

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

import java.util.Map;

@RestController
@RequestMapping("/api/oa/seal-applies")
public class SealController {
    private final SealService sealService;

    public SealController(SealService sealService) {
        this.sealService = sealService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long applicantId
    ) {
        return sealService.list(page, size, applicantId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        return sealService.detail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody SealDtos.SealCreateRequest request) {
        return sealService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody SealDtos.SealUpdateRequest request) {
        return sealService.update(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/submit")
    public Map<String, Object> submit(@PathVariable long id) {
        return sealService.submit(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable long id) {
        return sealService.withdrawSeal(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id) {
        return sealService.cancelSeal(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/return")
    public Map<String, Object> returnSeal(@PathVariable long id) {
        return sealService.returnSeal(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/export")
    public Map<String, Object> export(@RequestBody(required = false) Map<String, Object> filter) {
        return Map.of("message", "导出任务尚未接入", "filter", filter == null ? Map.of() : filter);
    }
}
