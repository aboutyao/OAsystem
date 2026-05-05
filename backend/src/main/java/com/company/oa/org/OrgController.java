package com.company.oa.org;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/org")
public class OrgController {
    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/depts/tree")
    public List<Map<String, Object>> deptTree() {
        return orgService.deptTree();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/depts/{id}")
    public Map<String, Object> deptDetail(@PathVariable long id) {
        return orgService.deptDetail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/depts")
    public Map<String, Object> createDept(@Valid @RequestBody OrgDtos.DeptCreateRequest request) {
        return orgService.createDept(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/depts/{id}")
    public Map<String, Object> updateDept(@PathVariable long id, @Valid @RequestBody OrgDtos.DeptUpdateRequest request) {
        return orgService.updateDept(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PatchMapping("/depts/{id}/enable")
    public Map<String, Object> enableDept(@PathVariable long id) {
        return orgService.setDeptStatus(id, "ENABLED");
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PatchMapping("/depts/{id}/disable")
    public Map<String, Object> disableDept(@PathVariable long id) {
        return orgService.setDeptStatus(id, "DISABLED");
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/depts/{id}/users")
    public PageResponse<Map<String, Object>> deptUsers(
            @PathVariable long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return orgService.deptUsers(id, page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/users")
    public PageResponse<Map<String, Object>> users(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long mainDeptId,
            @RequestParam(required = false) String employeeStatus,
            @RequestParam(required = false) String accountStatus
    ) {
        return orgService.listUsers(page, size, keyword, mainDeptId, employeeStatus, accountStatus);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/users/contacts")
    public List<Map<String, Object>> contacts(@RequestParam(required = false) String keyword) {
        return orgService.contacts(keyword);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/users/{id}")
    public Map<String, Object> user(@PathVariable long id) {
        return orgService.userDetail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/users")
    public Map<String, Object> createUser(@Valid @RequestBody OrgDtos.UserCreateRequest request) {
        return orgService.createUser(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable long id, @Valid @RequestBody OrgDtos.UserUpdateRequest request) {
        return orgService.updateUser(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PatchMapping("/users/{id}/enable")
    public Map<String, Object> enableUser(@PathVariable long id) {
        return orgService.enableUser(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PatchMapping("/users/{id}/disable")
    public Map<String, Object> disableUser(@PathVariable long id) {
        return orgService.disableUser(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PatchMapping("/users/{id}/resign")
    public Map<String, Object> resignUser(@PathVariable long id) {
        return orgService.resignUser(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'system:user:reset-password')")
    @PostMapping("/users/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable long id) {
        return orgService.resetPassword(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping(value = "/users/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportUsers() {
        String csv = orgService.exportUsersCsv();
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/users/import")
    public OrgDtos.UserImportResult importUsers(@RequestParam("file") MultipartFile file) {
        return orgService.importUsers(file);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/change-logs")
    public PageResponse<Map<String, Object>> changeLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String changeType
    ) {
        return orgService.listChangeLogs(page, size, targetType, changeType);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/positions")
    public List<Map<String, Object>> positions() {
        return orgService.listPositions();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/positions")
    public Map<String, Object> createPosition(@Valid @RequestBody OrgDtos.PositionUpsertRequest request) {
        return orgService.createPosition(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/positions/{id}")
    public Map<String, Object> updatePosition(@PathVariable long id, @Valid @RequestBody OrgDtos.PositionUpsertRequest request) {
        return orgService.updatePosition(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @DeleteMapping("/positions/{id}")
    public Map<String, Object> deletePosition(@PathVariable long id) {
        orgService.deletePosition(id);
        return Map.of("deleted", true, "id", id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/ranks")
    public List<Map<String, Object>> ranks() {
        return orgService.listRanks();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/ranks")
    public Map<String, Object> createRank(@Valid @RequestBody OrgDtos.RankUpsertRequest request) {
        return orgService.createRank(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/ranks/{id}")
    public Map<String, Object> updateRank(@PathVariable long id, @Valid @RequestBody OrgDtos.RankUpsertRequest request) {
        return orgService.updateRank(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @DeleteMapping("/ranks/{id}")
    public Map<String, Object> deleteRank(@PathVariable long id) {
        orgService.deleteRank(id);
        return Map.of("deleted", true, "id", id);
    }
}
