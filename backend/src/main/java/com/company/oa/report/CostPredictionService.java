package com.company.oa.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 成本预测服务
 * 基于历史数据预测：下季度采购预算、年度人力成本
 */
@Service
public class CostPredictionService {
    private final JdbcTemplate jdbcTemplate;

    public CostPredictionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 预测下季度采购成本
     */
    public Map<String, Object> predictQuarterlyPurchaseCost(int year, int quarter) {
        Map<String, Object> result = new HashMap<>();

        // 获取历史同期数据
        List<Map<String, Object>> historicalData = jdbcTemplate.queryForList(
            "SELECT QUARTER(created_at) as quarter, SUM(total_amount) as total " +
            "FROM oa_purchase WHERE YEAR(created_at) BETWEEN ? AND ? AND status != 'CANCELLED' " +
            "GROUP BY QUARTER(created_at)",
            year - 2, year - 1
        );

        // 计算平均季度采购额
        BigDecimal avgQuarterly = BigDecimal.ZERO;
        int count = 0;
        for (Map<String, Object> data : historicalData) {
            avgQuarterly = avgQuarterly.add((BigDecimal) data.get("total"));
            count++;
        }
        if (count > 0) {
            avgQuarterly = avgQuarterly.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);
        }

        // 获取今年已发生的采购额
        BigDecimal currentYearTotal = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_amount), 0) FROM oa_purchase WHERE YEAR(created_at) = ? AND status != 'CANCELLED'",
            BigDecimal.class, year
        );

        // 预测下季度
        result.put("historicalAvgQuarterly", avgQuarterly);
        result.put("currentYearTotal", currentYearTotal);
        result.put("predictedQuarterly", avgQuarterly.multiply(BigDecimal.valueOf(1.1))); // 假设增长10%
        result.put("confidenceLevel", calculateConfidence(historicalData.size()));

        return result;
    }

    /**
     * 预测年度人力成本
     */
    public Map<String, Object> predictAnnualLaborCost(int year) {
        Map<String, Object> result = new HashMap<>();

        // 获取当前员工数量
        Long employeeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM org_user WHERE deleted = 0", Long.class
        );

        // 获取当前月度报销总额
        BigDecimal monthlyExpense = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(total_amount), 0) FROM oa_expense WHERE YEAR(created_at) = ? AND MONTH(created_at) = MONTH(NOW())",
            BigDecimal.class, year
        );

        // 预测年度人力成本（简化模型）
        result.put("employeeCount", employeeCount);
        result.put("monthlyExpense", monthlyExpense);
        result.put("predictedAnnualCost", monthlyExpense.multiply(BigDecimal.valueOf(12)));
        result.put("predictedPerEmployee", monthlyExpense.multiply(BigDecimal.valueOf(12))
            .divide(BigDecimal.valueOf(employeeCount != null ? employeeCount : 1), 2, BigDecimal.ROUND_HALF_UP));

        return result;
    }

    /**
     * 部门成本分析
     */
    public List<Map<String, Object>> analyzeDepartmentCosts(int year) {
        return jdbcTemplate.queryForList(
            "SELECT d.name as deptName, " +
            "COALESCE(SUM(p.total_amount), 0) as purchaseAmount, " +
            "COALESCE(SUM(e.total_amount), 0) as expenseAmount, " +
            "COUNT(DISTINCT p.id) as purchaseCount, " +
            "COUNT(DISTINCT e.id) as expenseCount " +
            "FROM org_department d " +
            "LEFT JOIN oa_purchase p ON d.id = p.dept_id AND YEAR(p.created_at) = ? AND p.status != 'CANCELLED' " +
            "LEFT JOIN oa_expense e ON d.id = e.dept_id AND YEAR(e.created_at) = ? AND e.status != 'CANCELLED' " +
            "WHERE d.deleted = 0 " +
            "GROUP BY d.id, d.name " +
            "ORDER BY (COALESCE(SUM(p.total_amount), 0) + COALESCE(SUM(e.total_amount), 0)) DESC",
            year, year
        );
    }

    /**
     * 成本趋势分析
     */
    public List<Map<String, Object>> analyzeCostTrend(int year, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = months; i >= 1; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            BigDecimal purchaseAmount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM oa_purchase WHERE created_at BETWEEN ? AND ? AND status != 'CANCELLED'",
                BigDecimal.class, monthStart.atStartOfDay(), monthEnd.atStartOfDay()
            );

            BigDecimal expenseAmount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM oa_expense WHERE created_at BETWEEN ? AND ? AND status != 'CANCELLED'",
                BigDecimal.class, monthStart.atStartOfDay(), monthEnd.atStartOfDay()
            );

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.getMonthValue() + "月");
            monthData.put("purchaseAmount", purchaseAmount);
            monthData.put("expenseAmount", expenseAmount);
            monthData.put("totalAmount", purchaseAmount.add(expenseAmount));
            trend.add(monthData);
        }

        return trend;
    }

    private String calculateConfidence(int dataPoints) {
        if (dataPoints >= 8) return "高";
        if (dataPoints >= 4) return "中";
        return "低";
    }
}
