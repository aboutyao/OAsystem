package com.company.oa.workflow;

import com.company.oa.common.service.SequenceService;
import com.company.oa.workflow.mapper.WfTaskMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 任务依赖服务
 * 支持任务间依赖关系
 */
@Service
public class TaskDependencyService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final WfTaskMapper wfTaskMapper;

    public TaskDependencyService(JdbcTemplate jdbcTemplate, SequenceService sequenceService, WfTaskMapper wfTaskMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
        this.wfTaskMapper = wfTaskMapper;
    }

    /**
     * 创建任务依赖
     */
    @Transactional
    public void createDependency(long taskId, long dependsOnTaskId, String dependencyType) {
        long id = sequenceService.nextId("wf_task_dependency");
        jdbcTemplate.update(
            "INSERT INTO wf_task_dependency (id, task_id, depends_on_task_id, dependency_type, status, created_at) VALUES (?, ?, ?, ?, 'PENDING', NOW())",
            id, taskId, dependsOnTaskId, dependencyType
        );
    }

    /**
     * 检查任务是否可以开始
     */
    public boolean canStart(long taskId) {
        List<Map<String, Object>> dependencies = jdbcTemplate.queryForList(
            "SELECT d.status FROM wf_task_dependency d WHERE d.task_id = ? AND d.depends_on_task_id IN (SELECT t.id FROM wf_task t WHERE t.status != 'COMPLETED')",
            taskId
        );
        return dependencies.isEmpty();
    }

    /**
     * 获取任务的下游任务
     */
    public List<Map<String, Object>> getDownstreamTasks(long taskId) {
        return jdbcTemplate.queryForList(
            "SELECT t.id, t.node_name, t.assignee_name_snapshot, d.dependency_type FROM wf_task_dependency d JOIN wf_task t ON d.depends_on_task_id = t.id WHERE d.task_id = ?",
            taskId
        );
    }

    /**
     * 标记依赖完成
     */
    @Transactional
    public void markDependencyCompleted(long taskId) {
        jdbcTemplate.update(
            "UPDATE wf_task_dependency SET status = 'COMPLETED', completed_at = NOW() WHERE depends_on_task_id = ? AND status = 'PENDING'",
            taskId
        );

        // 检查下游任务是否可以开始
        List<Map<String, Object>> readyTasks = jdbcTemplate.queryForList(
            "SELECT DISTINCT d.task_id FROM wf_task_dependency d WHERE d.depends_on_task_id = ? AND d.status = 'COMPLETED' AND NOT EXISTS (SELECT 1 FROM wf_task_dependency dd WHERE dd.task_id = d.task_id AND dd.status = 'PENDING')",
            taskId
        );

        for (Map<String, Object> task : readyTasks) {
            Long downstreamTaskId = ((Number) task.get("task_id")).longValue();
            // 可以在这里触发下游任务的通知
        }
    }
}
