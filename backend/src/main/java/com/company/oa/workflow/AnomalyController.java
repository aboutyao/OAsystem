package com.company.oa.workflow;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow/anomalies")
public class AnomalyController {
    private final AnomalyDetectionService anomalyDetectionService;
    private final AuthService authService;

    public AnomalyController(AnomalyDetectionService anomalyDetectionService, AuthService authService) {
        this.anomalyDetectionService = anomalyDetectionService;
        this.authService = authService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping
    public List<AnomalyDetectionService.AnomalyRecord> detect(@RequestParam Long userId) {
        return anomalyDetectionService.detectAnomalies(userId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping("/self")
    public List<AnomalyDetectionService.AnomalyRecord> detectSelf() {
        AuthUser user = authService.currentUser();
        return anomalyDetectionService.detectAnomalies(user.id());
    }
}
