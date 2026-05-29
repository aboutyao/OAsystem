package com.company.oa.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow/analytics")
public class WorkflowAnalyticsController {
    private final WorkflowAnalyticsService analyticsService;

    public WorkflowAnalyticsController(WorkflowAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/bottlenecks")
    public WorkflowAnalyticsService.BottleneckReport getBottlenecks(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        if (startTime == null) startTime = LocalDateTime.now().minusDays(30);
        if (endTime == null) endTime = LocalDateTime.now();
        return analyticsService.analyzeBottlenecks(startTime, endTime);
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/efficiency")
    public Map<String, Object> getEfficiency(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.getDepartmentEfficiency(1L, days);
    }
}
