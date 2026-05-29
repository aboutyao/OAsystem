package com.company.oa.system;

import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API开放平台服务
 * 提供OpenAPI，支持第三方开发
 */
@Service
public class OpenApiService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;

    public OpenApiService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 注册API应用
     */
    @Transactional
    public Map<String, Object> registerApp(String appName, String description, String callbackUrl) {
        long id = sequenceService.nextId("open_api_app");
        String appKey = generateAppKey();
        String appSecret = generateAppSecret();

        jdbcTemplate.update(
            "INSERT INTO open_api_app (id, app_name, description, app_key, app_secret, callback_url, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', NOW())",
            id, appName, description, appKey, appSecret, callbackUrl
        );

        return Map.of(
            "appId", id,
            "appKey", appKey,
            "appSecret", appSecret,
            "status", "ACTIVE"
        );
    }

    /**
     * 获取访问令牌
     */
    public Map<String, Object> getAccessToken(String appKey, String appSecret) {
        // 验证应用
        Long appId = jdbcTemplate.queryForObject(
            "SELECT id FROM open_api_app WHERE app_key = ? AND app_secret = ? AND status = 'ACTIVE'",
            Long.class, appKey, appSecret
        );

        if (appId == null) {
            return Map.of("error", "无效的应用凭证");
        }

        // 生成访问令牌
        String accessToken = generateAccessToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        jdbcTemplate.update(
            "INSERT INTO open_api_token (id, app_id, access_token, expires_at, created_at) VALUES (?, ?, ?, ?, NOW())",
            generateId(), appId, accessToken, expiresAt
        );

        // 更新调用次数
        jdbcTemplate.update(
            "UPDATE open_api_app SET last_used_at = NOW(), call_count = call_count + 1 WHERE id = ?",
            appId
        );

        return Map.of(
            "accessToken", accessToken,
            "expiresIn", 7200,
            "tokenType", "Bearer"
        );
    }

    /**
     * 验证访问令牌
     */
    public Long validateAccessToken(String accessToken) {
        Long appId = jdbcTemplate.queryForObject(
            "SELECT app_id FROM open_api_token WHERE access_token = ? AND expires_at > NOW()",
            Long.class, accessToken
        );
        return appId;
    }

    /**
     * 获取API调用统计
     */
    public Map<String, Object> getApiStats(Long appId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCalls", jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(call_count), 0) FROM open_api_app WHERE id = ?", Long.class, appId
        ));
        stats.put("todayCalls", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM open_api_log WHERE app_id = ? AND created_at >= CURDATE()", Long.class, appId
        ));
        stats.put("errorCount", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM open_api_log WHERE app_id = ? AND status_code >= 400 AND created_at >= CURDATE()", Long.class, appId
        ));

        return stats;
    }

    /**
     * 获取API文档
     */
    public Map<String, Object> getApiDocumentation() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("version", "1.0");
        doc.put("baseUrl", "/api/open/v1");
        doc.put("endpoints", List.of(
            Map.of("path", "/users", "method", "GET", "description", "获取用户列表"),
            Map.of("path", "/leaves", "method", "GET", "description", "获取请假列表"),
            Map.of("path", "/expenses", "method", "GET", "description", "获取报销列表"),
            Map.of("path", "/workflow/instances", "method", "GET", "description", "获取流程实例")
        ));
        return doc;
    }

    private String generateAppKey() {
        return "oa_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String generateAppSecret() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 48);
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private Long generateId() {
        return System.currentTimeMillis() * 1000 + new Random().nextInt(1000);
    }
}
