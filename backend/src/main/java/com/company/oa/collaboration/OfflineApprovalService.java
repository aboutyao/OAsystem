package com.company.oa.collaboration;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.service.SequenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 离线审批服务
 * 支持离线缓存待办，联网自动同步
 */
@Service
public class OfflineApprovalService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public OfflineApprovalService(JdbcTemplate jdbcTemplate, SequenceService sequenceService, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 同步待办到本地
     */
    public List<Map<String, Object>> syncTodos(long userId, LocalDateTime lastSyncTime) {
        return jdbcTemplate.queryForList(
            "SELECT t.*, i.title as instance_title, i.business_type FROM wf_task t " +
            "LEFT JOIN wf_process_instance i ON t.wf_instance_id = i.id " +
            "WHERE t.assignee_id = ? AND t.status = 'PENDING' AND t.created_at > ? " +
            "ORDER BY t.created_at DESC",
            userId, lastSyncTime
        );
    }

    /**
     * 缓存审批操作到本地
     */
    @Transactional
    public Map<String, Object> cacheApprovalOp(String taskId, String action, String comment, Long userId) {
        long id = sequenceService.nextId("offline_approval_cache");

        try {
            Map<String, Object> operation = new HashMap<>();
            operation.put("taskId", taskId);
            operation.put("action", action);
            operation.put("comment", comment);
            operation.put("userId", userId);
            operation.put("timestamp", LocalDateTime.now().toString());

            String operationJson = objectMapper.writeValueAsString(operation);

            jdbcTemplate.update(
                "INSERT INTO offline_approval_cache (id, task_id, action, comment, user_id, status, operation_data, created_at) VALUES (?, ?, ?, ?, ?, 'PENDING', ?, NOW())",
                id, taskId, action, comment, userId, operationJson
            );

            return Map.of("id", id, "status", "PENDING");
        } catch (Exception e) {
            throw new RuntimeException("缓存审批操作失败", e);
        }
    }

    /**
     * 同步离线操作
     */
    @Transactional
    public Map<String, Object> syncOfflineOperations(long userId) {
        List<Map<String, Object>> pendingOps = jdbcTemplate.queryForList(
            "SELECT * FROM offline_approval_cache WHERE user_id = ? AND status = 'PENDING' ORDER BY created_at ASC",
            userId
        );

        int successCount = 0;
        int failCount = 0;

        for (Map<String, Object> op : pendingOps) {
            try {
                // 这里应该调用实际的审批服务
                // 暂时只更新状态
                jdbcTemplate.update(
                    "UPDATE offline_approval_cache SET status = 'SYNCED', synced_at = NOW() WHERE id = ?",
                    op.get("id")
                );
                successCount++;
            } catch (Exception e) {
                jdbcTemplate.update(
                    "UPDATE offline_approval_cache SET status = 'FAILED', error_message = ? WHERE id = ?",
                    e.getMessage(), op.get("id")
                );
                failCount++;
            }
        }

        return Map.of("successCount", successCount, "failCount", failCount);
    }

    /**
     * 获取离线缓存状态
     */
    public Map<String, Object> getCacheStatus(long userId) {
        Long pendingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM offline_approval_cache WHERE user_id = ? AND status = 'PENDING'",
            Long.class, userId
        );
        Long syncedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM offline_approval_cache WHERE user_id = ? AND status = 'SYNCED'",
            Long.class, userId
        );
        Long failedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM offline_approval_cache WHERE user_id = ? AND status = 'FAILED'",
            Long.class, userId
        );

        return Map.of(
            "pendingCount", pendingCount != null ? pendingCount : 0,
            "syncedCount", syncedCount != null ? syncedCount : 0,
            "failedCount", failedCount != null ? failedCount : 0
        );
    }
}
