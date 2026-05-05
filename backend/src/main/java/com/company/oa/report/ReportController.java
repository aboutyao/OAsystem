package com.company.oa.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'audit:view')")
    @GetMapping("/workflow-efficiency")
    public Map<String, Object> workflowEfficiency(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.workflowEfficiency(from, to);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'audit:view')")
    @GetMapping("/todo-summary")
    public Map<String, Object> todoSummary() {
        return reportService.todoSummary();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'audit:view')")
    @GetMapping("/leave-summary")
    public Map<String, Object> leaveSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.leaveSummary(from, to);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'audit:view')")
    @GetMapping("/expense-summary")
    public Map<String, Object> expenseSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.expenseSummary(from, to);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'audit:view')")
    @GetMapping("/contract-summary")
    public Map<String, Object> contractSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.contractSummary(from, to);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'audit:view')")
    @GetMapping("/asset-summary")
    public Map<String, Object> assetSummary() {
        return reportService.assetSummary();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'audit:view')")
    @GetMapping("/user-summary")
    public Map<String, Object> userSummary() {
        return reportService.userSummary();
    }
}
