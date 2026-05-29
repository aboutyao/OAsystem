package com.company.oa.collaboration;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 讨论串服务
 * 支持审批单讨论、@提及
 */
@Service
public class CommentService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final AuthService authService;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public CommentService(JdbcTemplate jdbcTemplate, SequenceService sequenceService, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
        this.authService = authService;
    }

    /**
     * 发表评论
     */
    @Transactional
    public Map<String, Object> addComment(String entityType, Long entityId, String content, Long parentId) {
        AuthUser user = authService.currentUser();
        long id = sequenceService.nextId("discussion_comment");

        // 提取@提及
        List<String> mentions = extractMentions(content);

        jdbcTemplate.update(
            "INSERT INTO discussion_comment (id, entity_type, entity_id, content, parent_id, author_id, author_name, mentions, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
            id, entityType, entityId, content, parentId, user.id(), user.realName(), mentions.isEmpty() ? null : String.join(",", mentions)
        );

        // 发送@提及通知
        for (String mentionName : mentions) {
            sendMentionNotification(mentionName, content, entityType, entityId);
        }

        return Map.of("id", id, "createdAt", LocalDateTime.now());
    }

    /**
     * 获取讨论串
     */
    public List<Map<String, Object>> getComments(String entityType, Long entityId) {
        return jdbcTemplate.queryForList(
            "SELECT c.*, u.real_name as author_name FROM discussion_comment c LEFT JOIN org_user u ON c.author_id = u.id WHERE c.entity_type = ? AND c.entity_id = ? AND c.deleted = 0 ORDER BY c.created_at ASC",
            entityType, entityId
        );
    }

    /**
     * 删除评论（软删除）
     */
    @Transactional
    public void deleteComment(long commentId) {
        jdbcTemplate.update("UPDATE discussion_comment SET deleted = 1, deleted_at = NOW() WHERE id = ?", commentId);
    }

    private List<String> extractMentions(String content) {
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    private void sendMentionNotification(String username, String content, String entityType, Long entityId) {
        // 查找用户
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT id, real_name FROM org_user WHERE username = ? OR real_name = ?",
            username, username
        );

        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("id")).longValue();
            String userName = (String) user.get("real_name");

            // 发送通知
            jdbcTemplate.update(
                "INSERT INTO msg_message (id, sender_id, sender_name, receiver_id, receiver_name, title, content, msg_type, related_type, related_id, is_read, created_at) VALUES (?, 0, '系统', ?, ?, '讨论提及', ?, 'DISCUSSION', ?, ?, 0, NOW())",
                sequenceService.nextId("msg_message"), userId, userName,
                "有人在讨论中提到了你: " + truncate(content, 100),
                entityType, entityId
            );
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
