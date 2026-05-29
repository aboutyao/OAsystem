package com.company.oa.notification;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 智能推送策略服务
 * 智能推送：紧急事项立即推，普通事项汇总推
 */
@Service
public class SmartNotificationService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final AuthService authService;

    public SmartNotificationService(JdbcTemplate jdbcTemplate, SequenceService sequenceService, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
        this.authService = authService;
    }

    /**
     * 发送智能通知
     */
    public void sendSmartNotification(long userId, String title, String content, String priority, String relatedType, Long relatedId) {
        AuthUser user = authService.currentUser();

        // 根据优先级决定推送方式
        switch (priority) {
            case "URGENT":
                // 紧急：立即推送
                pushImmediately(userId, title, content, relatedType, relatedId);
                break;
            case "HIGH":
                // 高：5分钟内推送
                schedulePush(userId, title, content, relatedType, relatedId, 5);
                break;
            case "NORMAL":
                // 普通：汇总推送（每小时）
                queueForDigest(userId, title, content, relatedType, relatedId);
                break;
            case "LOW":
                // 低：每天汇总
                queueForDailyDigest(userId, title, content, relatedType, relatedId);
                break;
        }

        // 记录通知
        recordNotification(userId, title, content, priority, relatedType, relatedId);
    }

    private void pushImmediately(long userId, String title, String content, String relatedType, Long relatedId) {
        // 实时推送（WebSocket/SSE）
        jdbcTemplate.update(
            "INSERT INTO notification_push (id, user_id, title, content, related_type, related_id, push_type, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'IMMEDIATE', 'SENT', NOW())",
            sequenceService.nextId("notification_push"), userId, title, content, relatedType, relatedId
        );
    }

    private void schedulePush(long userId, String title, String content, String relatedType, Long relatedId, int delayMinutes) {
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(delayMinutes);

        jdbcTemplate.update(
            "INSERT INTO notification_push (id, user_id, title, content, related_type, related_id, push_type, scheduled_at, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'SCHEDULED', ?, 'PENDING', NOW())",
            sequenceService.nextId("notification_push"), userId, title, content, relatedType, relatedId, scheduledTime
        );
    }

    private void queueForDigest(long userId, String title, String content, String relatedType, Long relatedId) {
        jdbcTemplate.update(
            "INSERT INTO notification_push (id, user_id, title, content, related_type, related_id, push_type, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'DIGEST', 'QUEUED', NOW())",
            sequenceService.nextId("notification_push"), userId, title, content, relatedType, relatedId
        );
    }

    private void queueForDailyDigest(long userId, String title, String content, String relatedType, Long relatedId) {
        jdbcTemplate.update(
            "INSERT INTO notification_push (id, user_id, title, content, related_type, related_id, push_type, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'DAILY', 'QUEUED', NOW())",
            sequenceService.nextId("notification_push"), userId, title, content, relatedType, relatedId
        );
    }

    private void recordNotification(long userId, String title, String content, String priority, String relatedType, Long relatedId) {
        jdbcTemplate.update(
            "INSERT INTO msg_message (id, sender_id, sender_name, receiver_id, receiver_name, title, content, msg_type, priority, related_type, related_id, is_read, created_at) VALUES (?, 0, '系统', ?, (SELECT real_name FROM org_user WHERE id = ?), ?, ?, 'NOTIFICATION', ?, ?, ?, 0, NOW())",
            sequenceService.nextId("msg_message"), userId, userId, title, content, priority, relatedType, relatedId
        );
    }

    /**
     * 获取用户通知设置
     */
    public Map<String, Object> getNotificationSettings(long userId) {
        List<Map<String, Object>> settings = jdbcTemplate.queryForList(
            "SELECT * FROM user_notification_setting WHERE user_id = ?", userId
        );

        if (settings.isEmpty()) {
            return getDefaultSettings();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> setting : settings) {
            result.put(String.valueOf(setting.get("notification_type")), setting.get("enabled"));
        }
        return result;
    }

    /**
     * 保存用户通知设置
     */
    public void saveNotificationSettings(long userId, Map<String, Boolean> settings) {
        for (Map.Entry<String, Boolean> entry : settings.entrySet()) {
            Long existingId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_notification_setting WHERE user_id = ? AND notification_type = ?",
                Long.class, userId, entry.getKey()
            );

            if (existingId != null) {
                jdbcTemplate.update(
                    "UPDATE user_notification_setting SET enabled = ?, updated_at = NOW() WHERE id = ?",
                    entry.getValue(), existingId
                );
            } else {
                jdbcTemplate.update(
                    "INSERT INTO user_notification_setting (id, user_id, notification_type, enabled, created_at) VALUES (?, ?, ?, ?, NOW())",
                    sequenceService.nextId("user_notification_setting"), userId, entry.getKey(), entry.getValue()
                );
            }
        }
    }

    private Map<String, Object> getDefaultSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("WORKFLOW_APPROVAL", true);
        settings.put("WORKFLOW_COMMENT", true);
        settings.put("LEAVE_APPROVAL", true);
        settings.put("EXPENSE_APPROVAL", true);
        settings.put("MESSAGE_MENTION", true);
        settings.put("SYSTEM_ANNOUNCEMENT", true);
        return settings;
    }
}
