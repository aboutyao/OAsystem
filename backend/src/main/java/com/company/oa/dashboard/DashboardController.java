package com.company.oa.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return dashboardService.summary();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/todos")
    public List<Map<String, Object>> todos(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.todos(limit);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/started")
    public List<Map<String, Object>> started(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.myStarted(limit);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/cc-to-me")
    public List<Map<String, Object>> ccToMe(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.ccToMe(limit);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notices")
    public List<Map<String, Object>> notices(@RequestParam(defaultValue = "10") int limit) {
        return dashboardService.recentNotices(limit);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/quick-actions")
    public List<Map<String, Object>> quickActions() {
        return dashboardService.quickActions();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/anomalies")
    public List<Map<String, Object>> anomalies() {
        return dashboardService.detectAnomalies();
    }
}
