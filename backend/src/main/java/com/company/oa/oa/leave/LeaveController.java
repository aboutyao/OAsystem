package com.company.oa.oa.leave;

import com.company.oa.common.api.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oa/leaves")
public class LeaveController {
    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long applicantId
    ) {
        return leaveService.list(page, size, applicantId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        return leaveService.detail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody LeaveDtos.LeaveCreateRequest request) {
        return leaveService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody LeaveDtos.LeaveUpdateRequest request) {
        return leaveService.update(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/submit")
    public Map<String, Object> submit(@PathVariable long id) {
        return leaveService.submit(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable long id) {
        return leaveService.withdrawLeave(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id) {
        return leaveService.cancelLeave(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/calculate-duration")
    public Map<String, Object> calculateDuration(
            @RequestParam String startAt,
            @RequestParam String endAt
    ) {
        return leaveService.calculateDuration(startAt, endAt);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/team-calendar")
    public List<Map<String, Object>> teamLeaveCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return leaveService.teamLeaveCalendar(start, end);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/export")
    public void exportLeaves(@RequestBody(required = false) Map<String, Object> filter, HttpServletResponse response) {
        leaveService.exportLeaves(filter, response);
    }
}
