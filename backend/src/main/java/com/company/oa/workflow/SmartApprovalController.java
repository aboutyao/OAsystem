package com.company.oa.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow/smart-approvals")
public class SmartApprovalController {
    private final SmartApprovalService smartApprovalService;

    public SmartApprovalController(SmartApprovalService smartApprovalService) {
        this.smartApprovalService = smartApprovalService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:approve')")
    @GetMapping("/recommend")
    public List<SmartApprovalService.SmartApprover> recommend(
            @RequestParam String roleCode,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Double amount) {
        return smartApprovalService.recommendApprovers(roleCode, businessType, amount);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:approve')")
    @GetMapping("/recommend/{roleCode}")
    public List<SmartApprovalService.SmartApprover> recommendByRole(@PathVariable String roleCode) {
        return smartApprovalService.recommendApprovers(roleCode, null, null);
    }
}
