package com.company.oa.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow/summary")
public class WorkflowSummaryController {
    private final WorkflowSummaryService summaryService;

    public WorkflowSummaryController(WorkflowSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping("/{wfInstanceId}")
    public String generateSummary(@PathVariable long wfInstanceId) {
        return summaryService.generateSummary(wfInstanceId);
    }
}
