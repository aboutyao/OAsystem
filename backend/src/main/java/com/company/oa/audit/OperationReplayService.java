package com.company.oa.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 操作回放服务
 * 关键操作可回放，像视频一样查看操作过程
 */
@Service
public class OperationReplayService {
    private final JdbcTemplate jdbcTemplate;

    public OperationReplayService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 记录操作快照
     */
    public void recordSnapshot(String entityType, Long entityId, String action, Object beforeState, Object afterState, Long operatorId) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String beforeJson = beforeState != null ? mapper.writeValueAsString(beforeState) : null;
            String afterJson = afterState != null ? mapper.writeValueAsString(afterState) : null;

            jdbcTemplate.update(
                "INSERT INTO operation_replay (id, entity_type, entity_id, action, before_state, after_state, operator_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                generateId(), entityType, entityId, action, beforeJson, afterJson, operatorId
            );
        } catch (Exception e) {
            // 记录失败不影响业务
        }
    }

    /**
     * 获取操作历史
     */
    public List<Map<String, Object>> getOperationHistory(String entityType, Long entityId) {
        return jdbcTemplate.queryForList(
            "SELECT r.*, u.real_name as operator_name FROM operation_replay r LEFT JOIN org_user u ON r.operator_id = u.id WHERE r.entity_type = ? AND r.entity_id = ? ORDER BY r.created_at ASC",
            entityType, entityId
        );
    }

    /**
     * 获取操作回放数据（按时间序列）
     */
    public List<ReplayFrame> getReplayFrames(String entityType, Long entityId) {
        List<Map<String, Object>> records = getOperationHistory(entityType, entityId);
        List<ReplayFrame> frames = new ArrayList<>();

        for (Map<String, Object> record : records) {
            ReplayFrame frame = new ReplayFrame();
            frame.setTimestamp((LocalDateTime) record.get("created_at"));
            frame.setAction((String) record.get("action"));
            frame.setOperatorName((String) record.get("operator_name"));
            frame.setBeforeState((String) record.get("before_state"));
            frame.setAfterState((String) record.get("after_state"));
            frames.add(frame);
        }

        return frames;
    }

    private Long generateId() {
        return System.currentTimeMillis() * 1000 + new Random().nextInt(1000);
    }

    public static class ReplayFrame {
        private LocalDateTime timestamp;
        private String action;
        private String operatorName;
        private String beforeState;
        private String afterState;

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getOperatorName() { return operatorName; }
        public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
        public String getBeforeState() { return beforeState; }
        public void setBeforeState(String beforeState) { this.beforeState = beforeState; }
        public String getAfterState() { return afterState; }
        public void setAfterState(String afterState) { this.afterState = afterState; }
    }
}
