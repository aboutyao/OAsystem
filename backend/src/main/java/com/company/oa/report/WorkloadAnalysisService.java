package com.company.oa.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 人员工作负荷分析服务
 * 可视化每人待办数量、平均处理时长，避免过载
 */
@Service
public class WorkloadAnalysisService {
    private final JdbcTemplate jdbcTemplate;

    public WorkloadAnalysisService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取人员工作负荷排名
     */
    public List<Map<String, Object>> getWorkloadRanking() {
        return jdbcTemplate.queryForList(
            "SELECT u.id as userId, u.real_name as userName, d.name as deptName, " +
            "COALESCE(t.pending_count, 0) as pendingCount, " +
            "COALESCE(t.avg_response_hours, 0) as avgResponseHours, " +
            "COALESCE(t.total_completed, 0) as totalCompleted, " +
            "CASE WHEN COALESCE(t.pending_count, 0) > 10 THEN 'HIGH' " +
            "WHEN COALESCE(t.pending_count, 0) > 5 THEN 'MEDIUM' " +
            "ELSE 'LOW' END as workloadLevel " +
            "FROM org_user u " +
            "LEFT JOIN org_department d ON u.main_dept_id = d.id " +
            "LEFT JOIN (" +
            "  SELECT assignee_id, " +
            "    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_count, " +
            "    AVG(TIMESTAMPDIFF(HOUR, created_at, completed_at)) as avg_response_hours, " +
            "    COUNT(CASE WHEN status IN ('COMPLETED', 'REJECTED') THEN 1 END) as total_completed " +
            "  FROM wf_task GROUP BY assignee_id" +
            ") t ON u.id = t.assignee_id " +
            "WHERE u.deleted = 0 " +
            "ORDER BY pending_count DESC"
        );
    }

    /**
     * 获取部门工作负荷
     */
    public List<Map<String, Object>> getDepartmentWorkload() {
        return jdbcTemplate.queryForList(
            "SELECT d.id as deptId, d.name as deptName, " +
            "COUNT(DISTINCT u.id) as userCount, " +
            "COALESCE(SUM(t.pending_count), 0) as totalPending, " +
            "COALESCE(AVG(t.avg_response_hours), 0) as avgResponseHours, " +
            "CASE WHEN COALESCE(SUM(t.pending_count), 0) / COUNT(DISTINCT u.id) > 5 THEN 'HIGH' " +
            "WHEN COALESCE(SUM(t.pending_count), 0) / COUNT(DISTINCT u.id) > 2 THEN 'MEDIUM' " +
            "ELSE 'LOW' END as workloadLevel " +
            "FROM org_department d " +
            "JOIN org_user u ON d.id = u.main_dept_id AND u.deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT assignee_id, " +
            "    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_count, " +
            "    AVG(TIMESTAMPDIFF(HOUR, created_at, completed_at)) as avg_response_hours " +
            "  FROM wf_task GROUP BY assignee_id" +
            ") t ON u.id = t.assignee_id " +
            "WHERE d.deleted = 0 " +
            "GROUP BY d.id, d.name " +
            "ORDER BY totalPending DESC"
        );
    }

    /**
     * 获取个人工作负荷详情
     */
    public Map<String, Object> getUserWorkloadDetail(long userId) {
        Map<String, Object> result = new HashMap<>();

        // 用户基本信息
        Map<String, Object> user = jdbcTemplate.queryForMap(
            "SELECT id, real_name, main_dept_id FROM org_user WHERE id = ?", userId
        );
        result.put("user", user);

        // 待办任务
        List<Map<String, Object>> pendingTasks = jdbcTemplate.queryForList(
            "SELECT t.*, i.title as instance_title FROM wf_task t " +
            "LEFT JOIN wf_process_instance i ON t.wf_instance_id = i.id " +
            "WHERE t.assignee_id = ? AND t.status = 'PENDING' ORDER BY t.created_at ASC",
            userId
        );
        result.put("pendingTasks", pendingTasks);
        result.put("pendingCount", pendingTasks.size());

        // 响应时间统计
        Map<String, Object> responseStats = jdbcTemplate.queryForMap(
            "SELECT " +
            "AVG(TIMESTAMPDIFF(HOUR, created_at, completed_at)) as avgResponseHours, " +
            "MIN(TIMESTAMPDIFF(HOUR, created_at, completed_at)) as minResponseHours, " +
            "MAX(TIMESTAMPDIFF(HOUR, created_at, completed_at)) as maxResponseHours, " +
            "COUNT(*) as totalCompleted " +
            "FROM wf_task_record WHERE operator_id = ? AND action IN ('APPROVE', 'REJECT') AND completed_at IS NOT NULL",
            userId
        );
        result.put("responseStats", responseStats);

        // 近7天工作量趋势
        List<Map<String, Object>> weeklyTrend = jdbcTemplate.queryForList(
            "SELECT DATE(created_at) as date, " +
            "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed, " +
            "COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending " +
            "FROM wf_task WHERE assignee_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(created_at) ORDER BY date",
            userId
        );
        result.put("weeklyTrend", weeklyTrend);

        return result;
    }

    /**
     * 获取过载预警
     */
    public List<Map<String, Object>> getOverloadWarnings() {
        return jdbcTemplate.queryForList(
            "SELECT u.id as userId, u.real_name as userName, d.name as deptName, " +
            "COUNT(t.id) as pendingCount, " +
            "AVG(TIMESTAMPDIFF(HOUR, t.created_at, NOW())) as avgWaitHours " +
            "FROM wf_task t " +
            "JOIN org_user u ON t.assignee_id = u.id " +
            "LEFT JOIN org_department d ON u.main_dept_id = d.id " +
            "WHERE t.status = 'PENDING' " +
            "GROUP BY t.assignee_id " +
            "HAVING COUNT(t.id) > 10 OR AVG(TIMESTAMPDIFF(HOUR, t.created_at, NOW())) > 48 " +
            "ORDER BY pendingCount DESC"
        );
    }
}
