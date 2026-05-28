package com.company.oa.dashboard;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardInsightController {

    private final DashboardInsightService dashboardInsightService;
    private final AuthService authService;

    public DashboardInsightController(DashboardInsightService dashboardInsightService,
                                      AuthService authService) {
        this.dashboardInsightService = dashboardInsightService;
        this.authService = authService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/insights")
    public Map<String, Object> getInsights() {
        return dashboardInsightService.getInsights();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/track-action")
    public Map<String, Object> trackAction(@RequestParam String path) {
        AuthUser user = authService.currentUser();
        dashboardInsightService.trackAction(user.id(), path);
        return Map.of("status", "ok", "path", path);
    }
}
