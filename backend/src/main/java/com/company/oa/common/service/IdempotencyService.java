package com.company.oa.common.service;

import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 幂等性服务
 * 防止重复提交
 */
@Service
public class IdempotencyService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;

    public IdempotencyService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 检查请求是否已处理
     * @param key 幂等键
     * @param userId 用户ID
     * @return 已处理的响应体，null表示未处理
     */
    public String checkAndGetResponse(String key, long userId) {
        try {
            Map<String, Object> record = jdbcTemplate.queryForMap(
                "SELECT response_body, status FROM idempotency_key WHERE idempotency_key = ? AND user_id = ? AND expires_at > NOW()",
                key, userId
            );
            if ("COMPLETED".equals(record.get("status"))) {
                return (String) record.get("response_body");
            }
        } catch (Exception e) {
            // 表不存在或查询失败，忽略
        }
        return null;
    }

    /**
     * 标记请求开始处理
     */
    public void markProcessing(String key, long userId, String requestPath) {
        try {
            long id = sequenceService.nextId("idempotency_key");
            jdbcTemplate.update(
                "INSERT INTO idempotency_key (id, idempotency_key, user_id, request_path, status, created_at, expires_at) VALUES (?, ?, ?, ?, 'PROCESSING', NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR)) ON DUPLICATE KEY UPDATE status = 'PROCESSING'",
                id, key, userId, requestPath
            );
        } catch (Exception e) {
            // 表不存在，忽略
        }
    }

    /**
     * 标记请求处理完成
     */
    public void markCompleted(String key, long userId, String responseBody) {
        try {
            jdbcTemplate.update(
                "UPDATE idempotency_key SET status = 'COMPLETED', response_body = ? WHERE idempotency_key = ? AND user_id = ?",
                responseBody, key, userId
            );
        } catch (Exception e) {
            // 表不存在，忽略
        }
    }

    /**
     * 标记请求处理失败
     */
    public void markFailed(String key, long userId) {
        try {
            jdbcTemplate.update(
                "UPDATE idempotency_key SET status = 'FAILED' WHERE idempotency_key = ? AND user_id = ?",
                key, userId
            );
        } catch (Exception e) {
            // 表不存在，忽略
        }
    }

    /**
     * 清理过期记录
     */
    public void cleanup() {
        try {
            jdbcTemplate.update("DELETE FROM idempotency_key WHERE expires_at < NOW()");
        } catch (Exception e) {
            // 表不存在，忽略
        }
    }
}
