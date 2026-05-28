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
public class AuditTrailController {

    private final AuditService auditService;

    public AuditTrailController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Paginated audit trail for a specific entity.
     *
     * GET /api/audit/trail?entityType=OA_LEAVE&entityId=123&page=1&size=20
     */
    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/trail")
    public PageResponse<Map<String, Object>> trail(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return auditService.listTrailByEntity(page, size, entityType, entityId);
    }
}
