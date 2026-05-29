package com.company.oa.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 部门健康度服务
 * 综合指标：审批效率、人均产出、预算使用率、员工满意度
 */
@Service
public class DepartmentHealthService {
    private final JdbcTemplate jdbcTemplate;

    public DepartmentHealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取部门健康度评分
     */
    public Map<String, Object> getDepartmentHealthScore(long deptId) {
        Map<String, Object> result = new HashMap<>();
        result.put("deptId", deptId);

        // 1. 审批效率评分（30%）
        double approvalScore = calculateApprovalScore(deptId);
        result.put("approvalScore", approvalScore);

        // 2. 预算使用率评分（25%）
        double budgetScore = calculateBudgetScore(deptId);
        result.put("budgetScore", budgetScore);

        // 3. 人员满意度评分（25%）
        double satisfactionScore = calculateSatisfactionScore(deptId);
        result.put("satisfactionScore", satisfactionScore);

        // 4. 产出效率评分（20%）
        double productivityScore = calculateProductivityScore(deptId);
        result.put("productivityScore", productivityScore);

        // 综合评分
        double totalScore = approvalScore * 0.3 + budgetScore * 0.25
            + satisfactionScore * 0.25 + productivityScore * 0.2;
        result.put("totalScore", Math.round(totalScore * 10) / 10.0);

        // 健康等级
        String healthLevel = totalScore >= 80 ? "健康" :
            totalScore >= 60 ? "一般" : "需关注";
        result.put("healthLevel", healthLevel);

        // 改进建议
        result.put("suggestions", generateSuggestions(approvalScore, budgetScore, satisfactionScore, productivityScore));

        return result;
    }

    private double calculateApprovalScore(long deptId) {
        // 统计部门平均审批时长
        Double avgHours = jdbcTemplate.queryForObject(
            "SELECT AVG(TIMESTAMPDIFF(HOUR, t.created_at, t.completed_at)) " +
            "FROM wf_task t " +
            "JOIN wf_process_instance i ON t.wf_instance_id = i.id " +
            "WHERE i.starter_dept_id = ? AND t.status IN ('COMPLETED', 'REJECTED')",
            Double.class, deptId
        );

        if (avgHours == null) return 80;

        // 评分：24小时内100分，每多24小时扣10分
        return Math.max(0, 100 - (avgHours / 24) * 10);
    }

    private double calculateBudgetScore(long deptId) {
        // 获取部门预算使用率
        BigDecimal budgetAmount = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(budget_amount), 0) FROM oa_budget WHERE dept_id = ? AND status = 'ACTIVE'",
            BigDecimal.class, deptId
        );

        BigDecimal usedAmount = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(used_amount), 0) FROM oa_budget WHERE dept_id = ? AND status = 'ACTIVE'",
            BigDecimal.class, deptId
        );

        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) == 0) return 80;

        double usageRate = usedAmount.doubleValue() / budgetAmount.doubleValue();

        // 评分：使用率70-90%最佳，低于70%或高于90%扣分
        if (usageRate >= 0.7 && usageRate <= 0.9) return 100;
        if (usageRate < 0.7) return usageRate / 0.7 * 100;
        return Math.max(0, 100 - (usageRate - 0.9) * 500);
    }

    private double calculateSatisfactionScore(long deptId) {
        // 这里可以从员工满意度调查数据中获取
        // 简化实现：返回默认值
        return 75;
    }

    private double calculateProductivityScore(long deptId) {
        // 统计部门人均产出
        Long userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM org_user WHERE main_dept_id = ? AND deleted = 0",
            Long.class, deptId
        );

        Long completedTasks = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wf_task t " +
            "JOIN org_user u ON t.assignee_id = u.id " +
            "WHERE u.main_dept_id = ? AND t.status IN ('COMPLETED', 'REJECTED')",
            Long.class, deptId
        );

        if (userCount == null || userCount == 0) return 80;

        double tasksPerUser = completedTasks != null ? (double) completedTasks / userCount : 0;

        // 评分：人均完成10个任务为基准
        return Math.min(100, tasksPerUser / 10 * 100);
    }

    private List<String> generateSuggestions(double approval, double budget, double satisfaction, double productivity) {
        List<String> suggestions = new ArrayList<>();

        if (approval < 60) {
            suggestions.add("审批效率偏低，建议优化审批流程或增加审批人");
        }
        if (budget < 60) {
            suggestions.add("预算使用率异常，建议审查预算分配");
        }
        if (satisfaction < 60) {
            suggestions.add("员工满意度较低，建议开展团队建设活动");
        }
        if (productivity < 60) {
            suggestions.add("产出效率不足，建议优化工作分配");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("部门运行良好，继续保持");
        }

        return suggestions;
    }

    /**
     * 获取所有部门健康度排名
     */
    public List<Map<String, Object>> getDepartmentHealthRanking() {
        List<Map<String, Object>> departments = jdbcTemplate.queryForList(
            "SELECT id, name FROM org_department WHERE deleted = 0 AND parent_id IS NOT NULL"
        );

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> dept : departments) {
            long deptId = ((Number) dept.get("id")).longValue();
            Map<String, Object> health = getDepartmentHealthScore(deptId);
            health.put("deptName", dept.get("name"));
            ranking.add(health);
        }

        ranking.sort((a, b) -> Double.compare(
            (double) b.get("totalScore"),
            (double) a.get("totalScore")
        ));

        return ranking;
    }
}
