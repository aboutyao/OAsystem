package com.company.oa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.auth.mapper.AuthSqlMapper;
import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.entity.contract.ContractInfo;
import com.company.oa.entity.org.User;
import com.company.oa.entity.ops.JobTaskLog;
import com.company.oa.common.service.SequenceService;
import com.company.oa.message.MessageService;
import com.company.oa.ops.mapper.JobTaskLogMapper;
import com.company.oa.org.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized scheduler service for periodic system maintenance tasks.
 * All tasks are gated by the {@code scheduler.enabled} sys_config flag.
 * Each task logs its execution to the job_task_log table.
 */
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private static final String JOB_LOG_TRIGGER = "SCHEDULER";
    private static final String CONFIG_KEY_ENABLED = "scheduler.enabled";

    private final AuthSqlMapper authSqlMapper;
    private final ContractInfoMapper contractMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final com.company.oa.workflow.AnomalyDetectionService anomalyDetectionService;
    private final JobTaskLogMapper jobTaskLogMapper;
    private final SequenceService sequenceService;
    private final RedisTemplate<String, Object> redisTemplate;

    public SchedulerService(
            AuthSqlMapper authSqlMapper,
            ContractInfoMapper contractMapper,
            UserMapper userMapper,
            MessageService messageService,
            com.company.oa.workflow.AnomalyDetectionService anomalyDetectionService,
            JobTaskLogMapper jobTaskLogMapper,
            SequenceService sequenceService,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.authSqlMapper = authSqlMapper;
        this.contractMapper = contractMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.jobTaskLogMapper = jobTaskLogMapper;
        this.sequenceService = sequenceService;
        this.redisTemplate = redisTemplate;
    }

    // ─── Scheduled Tasks ──────────────────────────────────────────────

    /**
     * Daily at 2:00 AM — check for users with expired passwords and log warnings.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkPasswordExpiry() {
        String jobCode = "PASSWORD_EXPIRY_CHECK";
        if (!isSchedulerEnabled()) {
            log.debug("Scheduler disabled, skipping {}", jobCode);
            return;
        }
        JobTaskLog jobLog = startJob(jobCode, "密码过期检查");
        try {
            LocalDateTime now = LocalDateTime.now();
            List<User> expiredUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getDeleted, 0)
                            .eq(User::getAccountStatus, "ENABLED")
                            .isNotNull(User::getPasswordExpiresAt)
                            .le(User::getPasswordExpiresAt, now)
            );

            for (User user : expiredUsers) {
                messageService.send(user.getId(), "REMIND",
                        "密码已过期提醒",
                        "您的登录密码已于" + user.getPasswordExpiresAt().toLocalDate() + "过期，请尽快修改密码以确保账号安全。",
                        "SYSTEM", user.getId(), null);
                log.info("Password expiry notification sent to user {} ({})", user.getId(), user.getRealName());
            }

            endJob(jobLog, "SUCCESS", (long) expiredUsers.size(), 0L, null);
            log.info("Password expiry check completed, found {} users with expired passwords", expiredUsers.size());
        } catch (Exception e) {
            endJob(jobLog, "FAIL", 0L, 1L, e.getMessage());
            log.error("Password expiry check failed", e);
        }
    }

    /**
     * Every hour — scan and clean JWT blacklist entries that have exceeded
     * the 24-hour window. Although Redis TTL handles automatic expiration,
     * this provides a safety net for any entries with incorrect TTL.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredTokens() {
        String jobCode = "TOKEN_CLEANUP";
        if (!isSchedulerEnabled()) {
            log.debug("Scheduler disabled, skipping {}", jobCode);
            return;
        }
        JobTaskLog jobLog = startJob(jobCode, "令牌黑名单清理");
        try {
            // Redis keys auto-expire via TTL set in JwtBlacklistService.
            // Scan for any stale keys as a safety measure.
            Set<String> keys = redisTemplate.keys("jwt:blacklist:*");
            long staleCount = 0;
            if (keys != null) {
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                        staleCount++;
                    }
                }
            }

            endJob(jobLog, "SUCCESS", staleCount, 0L, null);
            log.info("Token blacklist cleanup completed, removed {} stale entries", staleCount);
        } catch (Exception e) {
            endJob(jobLog, "FAIL", 0L, 1L, e.getMessage());
            log.error("Token blacklist cleanup failed", e);
        }
    }

    /**
     * Daily at 9:00 AM — find contracts expiring within 30/7/3 days
     * and send tiered notifications to contract owners and admins.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkContractExpiry() {
        String jobCode = "CONTRACT_EXPIRY_CHECK";
        if (!isSchedulerEnabled()) {
            log.debug("Scheduler disabled, skipping {}", jobCode);
            return;
        }
        JobTaskLog jobLog = startJob(jobCode, "合同到期检查");
        try {
            LocalDate today = LocalDate.now();
            long totalNotified = 0;

            // Check at three warning tiers: 30, 7, and 3 days
            for (int days : new int[]{30, 7, 3}) {
                LocalDate threshold = today.plusDays(days);
                List<ContractInfo> contracts = contractMapper.selectList(
                        new LambdaQueryWrapper<ContractInfo>()
                                .eq(ContractInfo::getDeleted, 0)
                                .in(ContractInfo::getStatus, "SIGNED", "APPROVED")
                                .ge(ContractInfo::getEndDate, today)
                                .le(ContractInfo::getEndDate, threshold)
                );

                for (ContractInfo contract : contracts) {
                    long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, contract.getEndDate());
                    String urgency = days <= 3 ? "紧急" : days <= 7 ? "重要" : "提醒";
                    String message = String.format("【%s】合同「%s」(编号: %s) 将在%d天后到期(%s)，请及时处理续签。",
                            urgency, contract.getContractName(), contract.getContractNo(),
                            daysRemaining, contract.getEndDate());

                    // Notify contract owner
                    if (contract.getCreatedBy() != null) {
                        messageService.send(contract.getCreatedBy(), "REMIND",
                                "合同到期" + urgency, message,
                                "CONTRACT", contract.getId(), null);
                        totalNotified++;
                    }
                }
            }

            endJob(jobLog, "SUCCESS", totalNotified, 0L, null);
            log.info("Contract expiry check completed, sent {} notifications", totalNotified);
        } catch (Exception e) {
            endJob(jobLog, "FAIL", 0L, 1L, e.getMessage());
            log.error("Contract expiry check failed", e);
        }
    }

    /**
     * Every 6 hours — run anomaly detection across leave and expense data.
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void detectAnomalies() {
        String jobCode = "ANOMALY_DETECTION";
        if (!isSchedulerEnabled()) {
            log.debug("Scheduler disabled, skipping {}", jobCode);
            return;
        }
        JobTaskLog jobLog = startJob(jobCode, "异常行为检测");
        try {
            List<Map<String, Object>> anomalies = anomalyDetectionService.detectAnomalies();
            long anomalyCount = anomalies.size();

            // Notify all SUPER_ADMIN users with a summary message
            if (!anomalies.isEmpty()) {
                List<Long> adminIds = userMapper.selectAllUserIdsByRoleCode("SUPER_ADMIN");

                // Count anomalies by type for the summary
                Map<String, Long> typeCount = new java.util.LinkedHashMap<>();
                Map<String, Long> severityCount = new java.util.LinkedHashMap<>();
                for (Map<String, Object> anomaly : anomalies) {
                    String type = String.valueOf(anomaly.getOrDefault("type", "UNKNOWN"));
                    String severity = String.valueOf(anomaly.getOrDefault("severity", "MEDIUM"));
                    typeCount.merge(type, 1L, Long::sum);
                    severityCount.merge(severity, 1L, Long::sum);
                }

                // Build summary message
                StringBuilder summary = new StringBuilder();
                summary.append("系统检测到 ").append(anomalyCount).append(" 条异常行为:\n");
                typeCount.forEach((type, count) ->
                        summary.append("  - ").append(type).append(": ").append(count).append(" 条\n"));
                summary.append("严重程度分布: ");
                severityCount.forEach((severity, count) ->
                        summary.append(severity).append(" ").append(count).append("条; "));

                // Also include detailed per-anomaly messages for full context
                for (Map<String, Object> anomaly : anomalies) {
                    String severity = String.valueOf(anomaly.getOrDefault("severity", "MEDIUM"));
                    String message = String.valueOf(anomaly.getOrDefault("message", "检测到异常行为"));
                    String type = String.valueOf(anomaly.getOrDefault("type", "UNKNOWN"));
                    summary.append("\n[").append(severity).append("] ").append(type).append(": ").append(message);
                }

                String title = "【异常检测报告】检测到 " + anomalyCount + " 条异常行为";
                String content = summary.toString();

                for (Long adminId : adminIds) {
                    messageService.send(adminId, "REMIND", title, content,
                            "ANOMALY", null, null);
                }
                log.info("Anomaly alert sent to {} admin(s)", adminIds.size());
            }

            endJob(jobLog, "SUCCESS", anomalyCount, 0L, null);
            log.info("Anomaly detection completed, found {} anomalies", anomalyCount);
        } catch (Exception e) {
            endJob(jobLog, "FAIL", 0L, 1L, e.getMessage());
            log.error("Anomaly detection failed", e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private boolean isSchedulerEnabled() {
        String value = authSqlMapper.selectConfigValue(CONFIG_KEY_ENABLED);
        if (value == null || value.isBlank()) {
            // Default to enabled when no config exists
            return true;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private JobTaskLog startJob(String jobCode, String jobName) {
        JobTaskLog jobLog = new JobTaskLog();
        jobLog.setId(sequenceService.nextId("job_task_log"));
        jobLog.setJobCode(jobCode);
        jobLog.setJobName(jobName);
        jobLog.setStatus("RUNNING");
        jobLog.setStartAt(LocalDateTime.now());
        jobLog.setTriggeredBy(JOB_LOG_TRIGGER);
        jobTaskLogMapper.insert(jobLog);
        return jobLog;
    }

    private void endJob(JobTaskLog jobLog, String status, long successCount, long failCount, String failReason) {
        LocalDateTime now = LocalDateTime.now();
        long durationMs = java.time.Duration.between(jobLog.getStartAt(), now).toMillis();
        jobLog.setStatus(status);
        jobLog.setEndAt(now);
        jobLog.setDurationMs(durationMs);
        jobLog.setSuccessCount(successCount);
        jobLog.setFailCount(failCount);
        jobLog.setFailReason(failReason);
        jobTaskLogMapper.updateById(jobLog);
    }
}
