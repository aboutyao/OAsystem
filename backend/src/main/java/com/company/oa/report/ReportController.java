package com.company.oa.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CostPredictionService costPredictionService;
    private final WorkloadAnalysisService workloadAnalysisService;
    private final DepartmentHealthService departmentHealthService;

    public ReportController(ReportService reportService,
                           CostPredictionService costPredictionService,
                           WorkloadAnalysisService workloadAnalysisService,
                           DepartmentHealthService departmentHealthService) {
        this.reportService = reportService;
        this.costPredictionService = costPredictionService;
        this.workloadAnalysisService = workloadAnalysisService;
        this.departmentHealthService = departmentHealthService;
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

    // ==================== 成本预测 ====================

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/cost-prediction/quarterly")
    public Map<String, Object> predictQuarterly(
            @RequestParam int year,
            @RequestParam int quarter) {
        return costPredictionService.predictQuarterlyPurchaseCost(year, quarter);
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/cost-prediction/annual")
    public Map<String, Object> predictAnnual(@RequestParam int year) {
        return costPredictionService.predictAnnualLaborCost(year);
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/cost-prediction/trend")
    public List<Map<String, Object>> costTrend(
            @RequestParam int year,
            @RequestParam(defaultValue = "12") int months) {
        return costPredictionService.analyzeCostTrend(year, months);
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/cost-prediction/department")
    public List<Map<String, Object>> departmentCosts(@RequestParam int year) {
        return costPredictionService.analyzeDepartmentCosts(year);
    }

    // ==================== 工作负荷分析 ====================

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/workload/ranking")
    public List<Map<String, Object>> workloadRanking() {
        return workloadAnalysisService.getWorkloadRanking();
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/workload/department")
    public List<Map<String, Object>> departmentWorkload() {
        return workloadAnalysisService.getDepartmentWorkload();
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/workload/user/{userId}")
    public Map<String, Object> userWorkload(@PathVariable long userId) {
        return workloadAnalysisService.getUserWorkloadDetail(userId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/workload/overload-warnings")
    public List<Map<String, Object>> overloadWarnings() {
        return workloadAnalysisService.getOverloadWarnings();
    }

    // ==================== 部门健康度 ====================

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/department-health/{deptId}")
    public Map<String, Object> departmentHealth(@PathVariable long deptId) {
        return departmentHealthService.getDepartmentHealthScore(deptId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'report:view')")
    @GetMapping("/department-health/ranking")
    public List<Map<String, Object>> departmentHealthRanking() {
        return departmentHealthService.getDepartmentHealthRanking();
    }
}
