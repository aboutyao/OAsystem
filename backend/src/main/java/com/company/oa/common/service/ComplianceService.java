package com.company.oa.common.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 合规检查服务
 * 自动检查报销是否超标、采购是否走流程
 */
@Service
public class ComplianceService {
    private final JdbcTemplate jdbcTemplate;

    public ComplianceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 检查报销合规性
     */
    public ComplianceResult checkExpenseCompliance(Long expenseId) {
        ComplianceResult result = new ComplianceResult();
        result.setExpenseId(expenseId);
        result.setPassed(true);

        try {
            // 获取报销信息
            Map<String, Object> expense = jdbcTemplate.queryForMap(
                "SELECT * FROM oa_expense WHERE id = ?", expenseId
            );

            BigDecimal totalAmount = (BigDecimal) expense.get("total_amount");
            String expenseType = (String) expense.get("expense_type");
            Long createdBy = ((Number) expense.get("created_by")).longValue();

            // 检查1: 金额是否超标
            BigDecimal limit = getExpenseLimit(expenseType);
            if (limit != null && totalAmount.compareTo(limit) > 0) {
                result.setPassed(false);
                result.addViolation("AMOUNT_EXCEEDED", "金额超过" + expenseType + "限额 " + limit + " 元");
            }

            // 检查2: 是否有重复报销
            Long duplicateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oa_expense WHERE created_by = ? AND total_amount = ? AND id != ? AND status != 'CANCELLED'",
                Long.class, createdBy, totalAmount, expenseId
            );
            if (duplicateCount != null && duplicateCount > 0) {
                result.setPassed(false);
                result.addViolation("DUPLICATE_EXPENSE", "可能存在重复报销");
            }

            // 检查3: 月度总额是否超标
            BigDecimal monthlyTotal = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM oa_expense WHERE created_by = ? AND YEAR(created_at) = YEAR(NOW()) AND MONTH(created_at) = MONTH(NOW()) AND status != 'CANCELLED'",
                BigDecimal.class, createdBy
            );
            BigDecimal monthlyLimit = new BigDecimal("50000"); // 月度限额5万
            if (monthlyTotal.add(totalAmount).compareTo(monthlyLimit) > 0) {
                result.setPassed(false);
                result.addViolation("MONTHLY_EXCEEDED", "月度报销总额将超过 " + monthlyLimit + " 元");
            }

        } catch (Exception e) {
            result.setPassed(false);
            result.addViolation("CHECK_FAILED", "合规检查失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查采购合规性
     */
    public ComplianceResult checkPurchaseCompliance(Long purchaseId) {
        ComplianceResult result = new ComplianceResult();
        result.setPurchaseId(purchaseId);
        result.setPassed(true);

        try {
            Map<String, Object> purchase = jdbcTemplate.queryForMap(
                "SELECT * FROM oa_purchase WHERE id = ?", purchaseId
            );

            BigDecimal totalAmount = (BigDecimal) purchase.get("total_amount");
            String purchaseType = (String) purchase.get("purchase_type");

            // 检查1: 大额采购是否走审批流程
            BigDecimal approvalThreshold = new BigDecimal("10000"); // 1万以上需要审批
            if (totalAmount.compareTo(approvalThreshold) > 0) {
                Long hasApproval = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM wf_process_instance WHERE business_type = 'PURCHASE' AND business_id = ? AND status IN ('APPROVING', 'APPROVED')",
                    Long.class, purchaseId
                );
                if (hasApproval == null || hasApproval == 0) {
                    result.setPassed(false);
                    result.addViolation("NO_APPROVAL", "大额采购未走审批流程");
                }
            }

            // 检查2: 供应商是否有资质
            String supplierName = (String) purchase.get("supplier_name");
            if (supplierName != null) {
                // 这里可以检查供应商资质
            }

        } catch (Exception e) {
            result.setPassed(false);
            result.addViolation("CHECK_FAILED", "合规检查失败: " + e.getMessage());
        }

        return result;
    }

    private BigDecimal getExpenseLimit(String expenseType) {
        return switch (expenseType) {
            case "差旅" -> new BigDecimal("10000");
            case "招待" -> new BigDecimal("5000");
            case "办公" -> new BigDecimal("2000");
            default -> null;
        };
    }

    /**
     * 获取合规检查报告
     */
    public Map<String, Object> getComplianceReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();

        // 报销合规统计
        Long expenseViolations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM compliance_violation WHERE violation_type LIKE 'EXPENSE_%' AND created_at BETWEEN ? AND ?",
            Long.class, startTime, endTime
        );
        report.put("expenseViolations", expenseViolations != null ? expenseViolations : 0);

        // 采购合规统计
        Long purchaseViolations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM compliance_violation WHERE violation_type LIKE 'PURCHASE_%' AND created_at BETWEEN ? AND ?",
            Long.class, startTime, endTime
        );
        report.put("purchaseViolations", purchaseViolations != null ? purchaseViolations : 0);

        return report;
    }

    public static class ComplianceResult {
        private Long expenseId;
        private Long purchaseId;
        private boolean passed;
        private List<Map<String, String>> violations = new ArrayList<>();

        public Long getExpenseId() { return expenseId; }
        public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
        public Long getPurchaseId() { return purchaseId; }
        public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public List<Map<String, String>> getViolations() { return violations; }

        public void addViolation(String type, String message) {
            Map<String, String> violation = new HashMap<>();
            violation.put("type", type);
            violation.put("message", message);
            violations.add(violation);
        }
    }
}
