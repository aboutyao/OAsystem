package com.company.oa.common.service;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 批量操作服务
 * 支持批量审批、批量导出、批量打印
 */
@Service
public class BatchOperationService {
    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;

    public BatchOperationService(JdbcTemplate jdbcTemplate, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
    }

    /**
     * 批量审批
     */
    @Transactional
    public Map<String, Object> batchApprove(List<Long> taskIds, String comment, String action) {
        AuthUser user = authService.currentUser();
        int successCount = 0;
        int failCount = 0;

        for (Long taskId : taskIds) {
            try {
                // 获取任务信息
                Map<String, Object> task = jdbcTemplate.queryForMap(
                    "SELECT * FROM wf_task WHERE id = ? AND status = 'PENDING'",
                    taskId
                );

                if (task == null) {
                    failCount++;
                    continue;
                }

                // 更新任务状态
                jdbcTemplate.update(
                    "UPDATE wf_task SET status = ?, completed_at = NOW() WHERE id = ?",
                    action.equals("APPROVE") ? "COMPLETED" : "REJECTED",
                    taskId
                );

                // 记录审批
                jdbcTemplate.update(
                    "INSERT INTO wf_task_record (id, wf_instance_id, node_name, action, operator_id, operator_name, comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                    generateId(), task.get("wf_instance_id"), task.get("node_name"),
                    action, user.id(), user.realName(), comment
                );

                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }

        return Map.of("successCount", successCount, "failCount", failCount);
    }

    /**
     * 批量导出
     */
    public List<Map<String, Object>> batchExport(String entityType, List<Long> ids, List<String> fields) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(String.join(", ", fields));
        sql.append(" FROM ").append(entityType);
        sql.append(" WHERE id IN (");
        sql.append(String.join(",", ids.stream().map(String::valueOf).toList()));
        sql.append(")");

        return jdbcTemplate.queryForList(sql.toString());
    }

    /**
     * 批量删除（软删除）
     */
    @Transactional
    public Map<String, Object> batchSoftDelete(String tableName, List<Long> ids) {
        int count = jdbcTemplate.update(
            "UPDATE " + tableName + " SET deleted = 1, deleted_at = NOW() WHERE id IN (" +
            String.join(",", ids.stream().map(String::valueOf).toList()) + ")"
        );
        return Map.of("deletedCount", count);
    }

    /**
     * 批量状态更新
     */
    @Transactional
    public Map<String, Object> batchUpdateStatus(String tableName, List<Long> ids, String statusField, String statusValue) {
        int count = jdbcTemplate.update(
            "UPDATE " + tableName + " SET " + statusField + " = ?, updated_at = NOW() WHERE id IN (" +
            String.join(",", ids.stream().map(String::valueOf).toList()) + ")",
            statusValue
        );
        return Map.of("updatedCount", count);
    }

    private Long generateId() {
        return System.currentTimeMillis() * 1000 + new Random().nextInt(1000);
    }
}
