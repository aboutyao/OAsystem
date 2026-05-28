package com.company.oa.dashboard;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.message.MessageService;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardInsightService {

    private static final Logger log = LoggerFactory.getLogger(DashboardInsightService.class);
    private static final String ACTION_KEY_PREFIX = "dashboard:actions:";
    private static final int ACTION_TTL_DAYS = 90;

    private final WfTaskMapper wfTaskMapper;
    private final WfProcessInstanceMapper wfProcessInstanceMapper;
    private final MessageService messageService;
    private final ContractInfoMapper contractInfoMapper;
    private final AuthService authService;
    private final StringRedisTemplate redisTemplate;

    public DashboardInsightService(WfTaskMapper wfTaskMapper,
                                   WfProcessInstanceMapper wfProcessInstanceMapper,
                                   MessageService messageService,
                                   ContractInfoMapper contractInfoMapper,
                                   AuthService authService,
                                   StringRedisTemplate redisTemplate) {
        this.wfTaskMapper = wfTaskMapper;
        this.wfProcessInstanceMapper = wfProcessInstanceMapper;
        this.messageService = messageService;
        this.contractInfoMapper = contractInfoMapper;
        this.authService = authService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInsights() {
        AuthUser user = authService.currentUser();
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("briefing", buildBriefing(user));
        result.put("approvalVelocity", buildApprovalVelocity(user));
        result.put("upcomingDeadlines", buildUpcomingDeadlines(user));
        result.put("topActions", getTopActions(user));

        return result;
    }

    // ─── Daily Briefing ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String buildBriefing(AuthUser user) {
        long pendingApprovals = countPendingApprovals(user.id());
        long unreadMessages = messageService.countUnreadForUser(user.id());
        long expiringContracts = countExpiringContracts(7);

        StringBuilder sb = new StringBuilder();
        sb.append("Today you have ")
                .append(pendingApprovals).append(" pending approval").append(pendingApprovals == 1 ? "" : "s")
                .append(", ").append(unreadMessages).append(" unread message").append(unreadMessages == 1 ? "" : "s")
                .append(", ").append(expiringContracts).append(" contract").append(expiringContracts == 1 ? "" : "s")
                .append(" expiring soon.");

        if (pendingApprovals == 0 && unreadMessages == 0 && expiringContracts == 0) {
            sb.append(" All clear! You are on top of everything.");
        }

        return sb.toString();
    }

    // ─── Approval Velocity ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> buildApprovalVelocity(AuthUser user) {
        Map<String, Object> velocity = new LinkedHashMap<>();

        double avgHours = calculateUserAvgApprovalHours(user.id());
        double teamAvgHours = calculateTeamAvgApprovalHours(user.id());

        velocity.put("avgHours", roundToOneDecimal(avgHours));
        velocity.put("teamAvgHours", roundToOneDecimal(teamAvgHours));

        if (teamAvgHours > 0) {
            double ratio = avgHours / teamAvgHours;
            velocity.put("fasterThanTeam", ratio < 1.0);
            velocity.put("speedRatio", roundToOneDecimal(1.0 / Math.max(ratio, 0.01)));
        } else {
            velocity.put("fasterThanTeam", null);
            velocity.put("speedRatio", null);
        }

        return velocity;
    }

    private double calculateUserAvgApprovalHours(long userId) {
        Map<String, Object> result = wfTaskMapper.selectApprovalTimeStats(userId);
        if (result == null || result.get("avgHours") == null) {
            return 0.0;
        }
        return ((Number) result.get("avgHours")).doubleValue();
    }

    private double calculateTeamAvgApprovalHours(long currentUserId) {
        Map<String, Object> result = wfTaskMapper.selectTeamApprovalTimeStats(currentUserId);
        if (result == null || result.get("avgHours") == null) {
            return 0.0;
        }
        return ((Number) result.get("avgHours")).doubleValue();
    }

    // ─── Upcoming Deadlines ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildUpcomingDeadlines(AuthUser user) {
        List<Map<String, Object>> deadlines = new ArrayList<>();

        // Contracts expiring in 7 days
        List<Map<String, Object>> expiringContracts = contractInfoMapper.selectExpiringContracts(
                user.id(), LocalDate.now(), LocalDate.now().plusDays(7));
        if (expiringContracts != null) {
            for (Map<String, Object> contract : expiringContracts) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("type", "CONTRACT_EXPIRY");
                entry.put("title", "Contract: " + contract.getOrDefault("contractName", "Unknown"));
                entry.put("contractNo", contract.get("contractNo"));
                entry.put("endDate", contract.get("endDate") != null
                        ? contract.get("endDate").toString() : null);
                deadlines.add(entry);
            }
        }

        // SLA breach risks: instances whose sla_deadline is within 24h and not yet breached
        List<Map<String, Object>> slaBreaches = wfProcessInstanceMapper.selectSlaBreachesAtRisk(
                user.id(), LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        if (slaBreaches != null) {
            for (Map<String, Object> inst : slaBreaches) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("type", "SLA_BREACH_RISK");
                entry.put("title", inst.getOrDefault("title", "Unknown process"));
                entry.put("slaDeadline", inst.get("slaDeadline") != null
                        ? inst.get("slaDeadline").toString() : null);
                deadlines.add(entry);
            }
        }

        // SLA already breached
        List<Map<String, Object>> breached = wfProcessInstanceMapper.selectSlaBreachedInstances(user.id());
        if (breached != null) {
            for (Map<String, Object> inst : breached) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("type", "SLA_BREACHED");
                entry.put("title", inst.getOrDefault("title", "Unknown process"));
                entry.put("slaDeadline", inst.get("slaDeadline") != null
                        ? inst.get("slaDeadline").toString() : null);
                deadlines.add(entry);
            }
        }

        return deadlines;
    }

    // ─── Quick Action Tracking (Redis) ─────────────────────────────────

    public void trackAction(long userId, String path) {
        String key = ACTION_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForHash().increment(key, path, 1);
            redisTemplate.expire(key, ACTION_TTL_DAYS, java.util.concurrent.TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Failed to track action in Redis for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopActions(AuthUser user) {
        String key = ACTION_KEY_PREFIX + user.id();
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries == null || entries.isEmpty()) {
                return result;
            }

            // Sort by count descending, return top 5
            entries.entrySet().stream()
                    .sorted((a, b) -> Long.compare(
                            Long.parseLong(String.valueOf(b.getValue())),
                            Long.parseLong(String.valueOf(a.getValue()))))
                    .limit(5)
                    .forEach(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("path", String.valueOf(e.getKey()));
                        item.put("count", Long.parseLong(String.valueOf(e.getValue())));
                        result.add(item);
                    });
        } catch (Exception e) {
            log.warn("Failed to read top actions from Redis for user {}: {}", user.id(), e.getMessage());
        }

        return result;
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private long countPendingApprovals(long userId) {
        Long n = wfTaskMapper.countTodoTasks(userId, "PENDING");
        return n == null ? 0L : n;
    }

    private long countExpiringContracts(int daysAhead) {
        // Count all contracts expiring within N days across all users
        Long n = contractInfoMapper.countExpiringContracts(
                LocalDate.now(), LocalDate.now().plusDays(daysAhead));
        return n == null ? 0L : n;
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
