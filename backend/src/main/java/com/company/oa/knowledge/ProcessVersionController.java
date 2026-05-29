package com.company.oa.knowledge;

import com.company.oa.workflow.ProcessVersionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow/versions")
public class ProcessVersionController {
    private final ProcessVersionService versionService;

    public ProcessVersionController(ProcessVersionService versionService) {
        this.versionService = versionService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping("/template/{templateId}")
    public List<Map<String, Object>> listVersions(@PathVariable long templateId) {
        return versionService.listVersions(templateId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:manage')")
    @PostMapping("/template/{templateId}")
    public Map<String, Object> createVersion(
            @PathVariable long templateId,
            @RequestBody Map<String, String> request) {
        return versionService.createVersion(templateId, request.get("config"), request.get("changeDescription"));
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:manage')")
    @PostMapping("/{versionId}/publish")
    public Map<String, Object> publishVersion(@PathVariable long versionId) {
        return versionService.publishVersion(versionId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping("/compare")
    public Map<String, Object> compareVersions(
            @RequestParam long v1,
            @RequestParam long v2) {
        return versionService.compareVersions(v1, v2);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:manage')")
    @PostMapping("/{versionId}/rollback")
    public Map<String, Object> rollback(@PathVariable long versionId) {
        return versionService.rollbackToVersion(versionId);
    }
}
