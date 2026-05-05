package com.company.oa.report;

import com.company.oa.report.mapper.ReportSqlMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportService {

    private final ReportSqlMapper reportMapper;

    public ReportService(ReportSqlMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> workflowEfficiency(LocalDate from, LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        LocalDate nextDay = t.plusDays(1);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("from", f.toString());
        r.put("to", t.toString());
        r.put("totalInstances", Objects.requireNonNullElse(reportMapper.countWfInstancesByDateRange(f, nextDay), 0L));
        r.put("approving", Objects.requireNonNullElse(reportMapper.countWfInstancesByStatusAndDateRange("APPROVING", f, nextDay), 0L));
        r.put("approved", Objects.requireNonNullElse(reportMapper.countWfInstancesByStatusAndDateRange("APPROVED", f, nextDay), 0L));
        r.put("rejected", Objects.requireNonNullElse(reportMapper.countWfInstancesByStatusAndDateRange("REJECTED", f, nextDay), 0L));
        Double avgHours = reportMapper.avgWfProcessHours(f, nextDay);
        r.put("avgHours", avgHours == null ? 0.0 : Math.round(avgHours * 100.0) / 100.0);
        r.put("byBusinessType", reportMapper.groupWfInstancesByBusinessType(f, nextDay));
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> todoSummary() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pending", Objects.requireNonNullElse(reportMapper.countWfTasksByStatus("PENDING"), 0L));
        r.put("completed", Objects.requireNonNullElse(reportMapper.countWfTasksByStatus("COMPLETED"), 0L));
        r.put("timeout", Objects.requireNonNullElse(reportMapper.countWfTasksOverdue(), 0L));
        r.put("topAssignees", reportMapper.topTodoAssignees());
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> leaveSummary(LocalDate from, LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().minusDays(90) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        LocalDate nextDay = t.plusDays(1);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("from", f.toString());
        r.put("to", t.toString());
        r.put("totalCount", Objects.requireNonNullElse(reportMapper.countLeavesByDateRange(f, nextDay), 0L));
        r.put("approvedCount", Objects.requireNonNullElse(reportMapper.countLeavesByStatusAndDateRange("APPROVED", f, nextDay), 0L));
        Double totalDays = reportMapper.sumLeaveDaysApproved(f, nextDay);
        r.put("totalDays", totalDays == null ? 0.0 : totalDays);
        r.put("byLeaveType", reportMapper.groupLeavesByType(f, nextDay));
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> expenseSummary(LocalDate from, LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().minusDays(90) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        LocalDate nextDay = t.plusDays(1);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("from", f.toString());
        r.put("to", t.toString());
        r.put("totalCount", Objects.requireNonNullElse(reportMapper.countExpensesByDateRange(f, nextDay), 0L));
        r.put("totalAmount", reportMapper.sumExpenseTotalAmountApproved(f, nextDay));
        r.put("paidAmount", reportMapper.sumExpensePaidAmount(f, nextDay));
        r.put("byCategory", reportMapper.groupExpensesByCategory(f, nextDay));
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> contractSummary(LocalDate from, LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().minusDays(365) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("from", f.toString());
        r.put("to", t.toString());
        r.put("contractCount", Objects.requireNonNullElse(reportMapper.countContractsByDateRange(f, t), 0L));
        r.put("totalAmount", reportMapper.sumContractAmounts(f, t));
        r.put("byType", reportMapper.groupContractsByType(f, t));
        r.put("expiringSoon30Days", Objects.requireNonNullElse(reportMapper.countContractsExpiringSoon(), 0L));
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> assetSummary() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("assetCount", Objects.requireNonNullElse(reportMapper.countAssets(), 0L));
        r.put("idleCount", Objects.requireNonNullElse(reportMapper.countAssetsByStatus("IDLE"), 0L));
        r.put("inUseCount", Objects.requireNonNullElse(reportMapper.countAssetsByStatus("IN_USE"), 0L));
        r.put("repairingCount", Objects.requireNonNullElse(reportMapper.countAssetsByStatus("REPAIRING"), 0L));
        r.put("scrappedCount", Objects.requireNonNullElse(reportMapper.countAssetsByStatus("SCRAPPED"), 0L));
        r.put("totalPurchasePrice", reportMapper.sumAssetPurchaseAmounts());
        r.put("byCategory", reportMapper.groupAssetsByCategory());
        return r;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> userSummary() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalUsers", Objects.requireNonNullElse(reportMapper.countAllUsers(), 0L));
        r.put("activeUsers", Objects.requireNonNullElse(reportMapper.countActiveUsers(), 0L));
        r.put("totalDepts", Objects.requireNonNullElse(reportMapper.countActiveDepts(), 0L));
        r.put("byDept", reportMapper.groupUsersByDept());
        return r;
    }
}
