package com.company.oa.ops;

import com.company.oa.common.api.PageResponse;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.permission.cache.PermissionCacheService;
import com.company.oa.system.cache.SystemCacheService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ops")
public class OpsHealthController {

    private final UserMapper userMapper;
    private final OpsService opsService;
    private final PermissionCacheService permissionCacheService;
    private final SystemCacheService systemCacheService;

    public OpsHealthController(UserMapper userMapper, OpsService opsService,
                               PermissionCacheService permissionCacheService, SystemCacheService systemCacheService) {
        this.userMapper = userMapper;
        this.opsService = opsService;
        this.permissionCacheService = permissionCacheService;
        this.systemCacheService = systemCacheService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "oa-system",
                "time", OffsetDateTime.now()
        );
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/online-users")
    public List<Map<String, Object>> onlineUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        return userMapper.selectRecentLogins(threshold);
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/job-logs")
    public PageResponse<Map<String, Object>> jobLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String jobCode,
            @RequestParam(required = false) String status
    ) {
        return opsService.listJobLogs(page, size, jobCode, status);
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/exceptions")
    public PageResponse<Map<String, Object>> exceptions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String severity
    ) {
        return opsService.listExceptions(page, size, severity);
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/backup-records")
    public PageResponse<Map<String, Object>> backupRecords(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String backupType,
            @RequestParam(required = false) String status
    ) {
        return opsService.listBackupRecords(page, size, backupType, status);
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @PostMapping("/cache/refresh")
    public Map<String, Object> refreshCache() {
        permissionCacheService.invalidateAll();
        systemCacheService.invalidateAll();
        return Map.of("success", true, "refreshedAt", OffsetDateTime.now());
    }
}