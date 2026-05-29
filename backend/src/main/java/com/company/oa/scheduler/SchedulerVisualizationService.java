package com.company.oa.scheduler;

import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 定时任务可视化服务
 * 可视化配置定时任务，非技术人员也能用
 */
@Service
public class SchedulerVisualizationService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;

    public SchedulerVisualizationService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 获取所有定时任务
     */
    public List<Map<String, Object>> listScheduledTasks() {
        return jdbcTemplate.queryForList(
            "SELECT * FROM scheduled_task ORDER BY created_at DESC"
        );
    }

    /**
     * 创建定时任务
     */
    public Map<String, Object> createTask(String name, String description, String cronExpression, String taskType, String config) {
        long id = sequenceService.nextId("scheduled_task");

        jdbcTemplate.update(
            "INSERT INTO scheduled_task (id, name, description, cron_expression, task_type, config, status, last_run_at, next_run_at, created_at) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', NULL, NULL, NOW())",
            id, name, description, cronExpression, taskType, config
        );

        // 计算下次执行时间
        LocalDateTime nextRun = calculateNextRun(cronExpression);
        jdbcTemplate.update("UPDATE scheduled_task SET next_run_at = ? WHERE id = ?", nextRun, id);

        return Map.of("id", id, "nextRun", nextRun);
    }

    /**
     * 启用/禁用任务
     */
    public Map<String, Object> toggleTask(long taskId, boolean enable) {
        String status = enable ? "ACTIVE" : "PAUSED";
        jdbcTemplate.update("UPDATE scheduled_task SET status = ? WHERE id = ?", status, taskId);
        return Map.of("status", status);
    }

    /**
     * 手动执行任务
     */
    public Map<String, Object> executeTask(long taskId) {
        try {
            Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT * FROM scheduled_task WHERE id = ?", taskId
            );

            // 记录执行
            long executionId = sequenceService.nextId("task_execution");
            jdbcTemplate.update(
                "INSERT INTO task_execution (id, task_id, status, started_at) VALUES (?, ?, 'RUNNING', NOW())",
                executionId, taskId
            );

            // 执行任务
            String taskType = (String) task.get("task_type");
            executeTaskByType(taskType, (String) task.get("config"));

            // 更新执行状态
            jdbcTemplate.update(
                "UPDATE task_execution SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?",
                executionId
            );

            // 更新任务状态
            jdbcTemplate.update(
                "UPDATE scheduled_task SET last_run_at = NOW(), run_count = run_count + 1 WHERE id = ?",
                taskId
            );

            return Map.of("executionId", executionId, "status", "COMPLETED");
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取任务执行历史
     */
    public List<Map<String, Object>> getExecutionHistory(long taskId) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM task_execution WHERE task_id = ? ORDER BY started_at DESC LIMIT 50",
            taskId
        );
    }

    /**
     * 获取任务统计
     */
    public Map<String, Object> getTaskStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalTasks", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scheduled_task", Long.class
        ));
        stats.put("activeTasks", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scheduled_task WHERE status = 'ACTIVE'", Long.class
        ));
        stats.put("totalExecutions", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_execution", Long.class
        ));
        stats.put("successRate", jdbcTemplate.queryForObject(
            "SELECT CONCAT(ROUND(COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(*), 1), '%') FROM task_execution WHERE started_at > DATE_SUB(NOW(), INTERVAL 7 DAY)",
            String.class
        ));

        return stats;
    }

    private void executeTaskByType(String taskType, String config) {
        // 根据任务类型执行
        switch (taskType) {
            case "DATA_SYNC":
                // 数据同步
                break;
            case "REPORT_GENERATE":
                // 报表生成
                break;
            case "CLEANUP":
                // 数据清理
                break;
            case "NOTIFICATION":
                // 通知发送
                break;
        }
    }

    private LocalDateTime calculateNextRun(String cronExpression) {
        // 简化实现：返回1小时后
        return LocalDateTime.now().plusHours(1);
    }
}
