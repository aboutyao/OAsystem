package com.company.oa.workflow;

import com.company.oa.common.service.SequenceService;
import com.company.oa.message.MessageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 智能催办服务
 * 不是简单超时催办，而是分析最佳催办时机和方式
 */
@Service
public class SmartReminderService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final MessageService messageService;

    public SmartReminderService(JdbcTemplate jdbcTemplate, SequenceService sequenceService, MessageService messageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
        this.messageService = messageService;
    }

    /**
     * 分析最佳催办时机
     */
    public Map<String, Object> analyzeReminderTiming(long taskId) {
        Map<String, Object> task = jdbcTemplate.queryForMap(
            "SELECT t.*, i.title as instance_title FROM wf_task t LEFT JOIN wf_process_instance i ON t.wf_instance_id = i.id WHERE t.id = ?",
            taskId
        );

        long assigneeId = ((Number) task.get("assignee_id")).longValue();
        LocalDateTime createdAt = (LocalDateTime) task.get("created_at");

        // 获取审批人历史响应模式
        Map<String, Object> pattern = analyzeApproverPattern(assigneeId);

        // 计算最佳催办时间
        String bestTime = calculateBestReminderTime(pattern, createdAt);

        // 推荐催办方式
        String recommendMethod = recommendReminderMethod(pattern);

        return Map.of(
            "taskId", taskId,
            "assigneeId", assigneeId,
            "bestReminderTime", bestTime,
            "recommendMethod", recommendMethod,
            "pattern", pattern
        );
    }

    private Map<String, Object> analyzeApproverPattern(long userId) {
        Map<String, Object> pattern = new HashMap<>();

        // 获取历史响应时间分布
        List<Map<String, Object>> responseTimes = jdbcTemplate.queryForList(
            "SELECT HOUR(completed_at) as hour, COUNT(*) as count FROM wf_task_record " +
            "WHERE operator_id = ? AND action IN ('APPROVE', 'REJECT') AND completed_at IS NOT NULL " +
            "GROUP BY HOUR(completed_at) ORDER BY count DESC",
            userId
        );

        // 找出最活跃的时间段
        if (!responseTimes.isEmpty()) {
            pattern.put("mostActiveHour", responseTimes.get(0).get("hour"));
            pattern.put("mostActiveCount", responseTimes.get(0).get("count"));
        }

        // 获取平均响应时间
        Double avgHours = jdbcTemplate.queryForObject(
            "SELECT AVG(TIMESTAMPDIFF(HOUR, created_at, completed_at)) FROM wf_task_record " +
            "WHERE operator_id = ? AND action IN ('APPROVE', 'REJECT') AND completed_at IS NOT NULL",
            Double.class, userId
        );
        pattern.put("avgResponseHours", avgHours != null ? avgHours : 24.0);

        // 获取待办积压情况
        Long pendingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wf_task WHERE assignee_id = ? AND status = 'PENDING'",
            Long.class, userId
        );
        pattern.put("pendingCount", pendingCount != null ? pendingCount : 0);

        return pattern;
    }

    private String calculateBestReminderTime(Map<String, Object> pattern, LocalDateTime taskCreatedAt) {
        int mostActiveHour = pattern.containsKey("mostActiveHour")
            ? ((Number) pattern.get("mostActiveHour")).intValue()
            : 10;

        // 如果当前时间在最活跃时间之前，建议在那个时间催办
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() < mostActiveHour) {
            return "今天 " + mostActiveHour + ":00";
        }

        // 如果已经过了最活跃时间，建议明天
        return "明天 " + mostActiveHour + ":00";
    }

    private String recommendReminderMethod(Map<String, Object> pattern) {
        long pendingCount = pattern.containsKey("pendingCount")
            ? ((Number) pattern.get("pendingCount")).longValue()
            : 0;

        double avgHours = pattern.containsKey("avgResponseHours")
            ? ((Number) pattern.get("avgResponseHours")).doubleValue()
            : 24.0;

        // 根据情况推荐催办方式
        if (pendingCount > 10) {
            return "站内消息"; // 积压严重，不要太打扰
        } else if (avgHours > 48) {
            return "邮件"; // 响应慢，用正式方式
        } else {
            return "即时消息"; // 正常情况
        }
    }

    /**
     * 执行智能催办
     */
    public Map<String, Object> sendSmartReminder(long taskId) {
        Map<String, Object> timing = analyzeReminderTiming(taskId);

        Map<String, Object> task = jdbcTemplate.queryForMap(
            "SELECT t.*, i.title as instance_title FROM wf_task t LEFT JOIN wf_process_instance i ON t.wf_instance_id = i.id WHERE t.id = ?",
            taskId
        );

        long assigneeId = ((Number) task.get("assignee_id")).longValue();
        String title = (String) task.get("instance_title");
        String method = (String) timing.get("recommendMethod");

        // 发送催办消息
        String reminderContent = String.format("您有一条待办任务「%s」已等待较长时间，请尽快处理。", title);
        messageService.send(assigneeId, "WORKFLOW", "待办催办", reminderContent, "WORKFLOW", null, ((Number) task.get("wf_instance_id")).longValue());

        // 记录催办
        long reminderId = sequenceService.nextId("smart_reminder");
        jdbcTemplate.update(
            "INSERT INTO smart_reminder (id, task_id, assignee_id, remind_method, remind_content, status, created_at) VALUES (?, ?, ?, ?, ?, 'SENT', NOW())",
            reminderId, taskId, assigneeId, method, reminderContent
        );

        return Map.of(
            "taskId", taskId,
            "method", method,
            "status", "SENT"
        );
    }

    /**
     * 定时检查需要催办的任务
     */
    @Scheduled(cron = "0 0 10,15 * * MON-FRI") // 工作日10点和15点
    public void checkAndSendReminders() {
        // 查找超过24小时未处理的任务
        List<Map<String, Object>> overdueTasks = jdbcTemplate.queryForList(
            "SELECT t.id FROM wf_task t WHERE t.status = 'PENDING' AND t.created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)"
        );

        for (Map<String, Object> task : overdueTasks) {
            long taskId = ((Number) task.get("id")).longValue();

            // 检查今天是否已经催办过
            Long remindedToday = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM smart_reminder WHERE task_id = ? AND DATE(created_at) = CURDATE()",
                Long.class, taskId
            );

            if (remindedToday == null || remindedToday == 0) {
                try {
                    sendSmartReminder(taskId);
                } catch (Exception e) {
                    // 催办失败不影响其他任务
                }
            }
        }
    }
}
