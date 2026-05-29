package com.company.oa.audit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit/replay")
public class OperationReplayController {
    private final OperationReplayService replayService;

    public OperationReplayController(OperationReplayService replayService) {
        this.replayService = replayService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping
    public List<OperationReplayService.ReplayFrame> getReplayFrames(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return replayService.getReplayFrames(entityType, entityId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/history")
    public List<Map<String, Object>> getHistory(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return replayService.getOperationHistory(entityType, entityId);
    }
}
