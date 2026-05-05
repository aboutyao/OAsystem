package com.company.oa.audit;

import com.company.oa.common.api.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/login-logs")
    public PageResponse<Map<String, Object>> loginLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String result
    ) {
        return auditService.listLoginLogs(page, size, username, result);
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/operation-logs")
    public PageResponse<Map<String, Object>> operationLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long operatorId
    ) {
        return auditService.listOperationLogs(page, size, businessType, result, operatorId);
    }
}
