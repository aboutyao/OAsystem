package com.company.oa.ops;

import com.company.oa.common.service.SequenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Webhook服务
 * 支持配置Webhook，与其他系统联动
 */
@Service
public class WebhookService {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WebhookService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 注册Webhook
     */
    public Map<String, Object> registerWebhook(String name, String url, String eventType, String secret) {
        long id = sequenceService.nextId("webhook_config");
        jdbcTemplate.update(
            "INSERT INTO webhook_config (id, name, url, event_type, secret, status, created_at) VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW())",
            id, name, url, eventType, secret
        );
        return Map.of("id", id, "status", "ACTIVE");
    }

    /**
     * 触发Webhook
     */
    @Async
    public void triggerWebhooks(String eventType, Object payload) {
        List<Map<String, Object>> webhooks = jdbcTemplate.queryForList(
            "SELECT * FROM webhook_config WHERE event_type = ? AND status = 'ACTIVE'",
            eventType
        );

        for (Map<String, Object> webhook : webhooks) {
            try {
                String url = (String) webhook.get("url");
                String secret = (String) webhook.get("secret");

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Secret", secret != null ? secret : "")
                    .POST(HttpRequest.BodyPublishers.ofString(com.fasterxml.jackson.databind.ObjectMapper.class.cast(new com.fasterxml.jackson.databind.ObjectMapper()).writeValueAsString(payload)))
                    .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        log.info("Webhook triggered: {} -> {}", url, response.statusCode());
                        recordDelivery((Long) webhook.get("id"), "SUCCESS", response.statusCode());
                    })
                    .exceptionally(e -> {
                        log.error("Webhook failed: {}", url, e);
                        recordDelivery((Long) webhook.get("id"), "FAILED", 0);
                        return null;
                    });
            } catch (Exception e) {
                log.error("Error triggering webhook", e);
            }
        }
    }

    private void recordDelivery(long webhookId, String status, int statusCode) {
        try {
            long id = sequenceService.nextId("webhook_delivery");
            jdbcTemplate.update(
                "INSERT INTO webhook_delivery (id, webhook_id, status, response_code, delivered_at) VALUES (?, ?, ?, ?, NOW())",
                id, webhookId, status, statusCode
            );
        } catch (Exception e) {
            log.error("Error recording webhook delivery", e);
        }
    }

    /**
     * 获取Webhook列表
     */
    public List<Map<String, Object>> listWebhooks() {
        return jdbcTemplate.queryForList("SELECT * FROM webhook_config WHERE status = 'ACTIVE'");
    }

    /**
     * 删除Webhook
     */
    public void deleteWebhook(long id) {
        jdbcTemplate.update("UPDATE webhook_config SET status = 'INACTIVE' WHERE id = ?", id);
    }
}
