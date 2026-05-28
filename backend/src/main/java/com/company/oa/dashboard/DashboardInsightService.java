package com.company.oa.dashboard;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.message.MessageService;
import com.company.oa.oa.mapper.LeaveBalanceMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    private final OaLeaveMapper oaLeaveMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;

    public DashboardInsightService(WfTaskMapper wfTaskMapper,
                                   WfProcessInstanceMapper wfProcessInstanceMapper,
                                   MessageService messageService,
                                   ContractInfoMapper contractInfoMapper,
                                   AuthService authService,
                                   StringRedisTemplate redisTemplate,
                                   OaLeaveMapper oaLeaveMapper,
                                   LeaveBalanceMapper leaveBalanceMapper) {
        this.wfTaskMapper = wfTaskMapper;
        this.wfProcessInstanceMapper = wfProcessInstanceMapper;
        this.messageService = messageService;
        this.contractInfoMapper = contractInfoMapper;
        this.authService = authService;
        this.redisTemplate = redisTemplate;
        this.oaLeaveMapper = oaLeaveMapper;
        this.leaveBalanceMapper = leaveBalanceMapper;
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

    // ─── Predictive Insights ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getPredictiveInsights(long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("leaveBalanceForecast", buildLeaveBalanceForecast(userId));
        result.put("contractExpiryAlert", buildContractExpiryAlert());
        result.put("approvalWorkloadPrediction", buildApprovalWorkloadPrediction(userId));
        result.put("slaRiskCount", buildSlaRiskCount(userId));

        return result;
    }

    // ─── Leave Balance Forecast ──────────────────────────────────────────

    private Map<String, Object> buildLeaveBalanceForecast(long userId) {
        Map<String, Object> forecast = new LinkedHashMap<>();
        List<Map<String, Object>> leaveTypes = new ArrayList<>();

        // Query current year leave balance from leave_balance table
        int currentYear = YearMonth.now().getYear();
        List<com.company.oa.entity.oa.LeaveBalance> balances =
                leaveBalanceMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.company.oa.entity.oa.LeaveBalance>()
                                .eq(com.company.oa.entity.oa.LeaveBalance::getUserId, userId)
                                .eq(com.company.oa.entity.oa.LeaveBalance::getYear, currentYear)
                                .eq(com.company.oa.entity.oa.LeaveBalance::getDeleted, 0)
                );

        // Query leave usage history for last 3 months
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<Map<String, Object>> usageHistory = oaLeaveMapper.selectLeaveUsageHistory(userId, threeMonthsAgo);

        // Build a map of leaveType -> monthly usage
        Map<String, List<Double>> monthlyUsageByType = new LinkedHashMap<>();
        for (Map<String, Object> record : usageHistory) {
            String type = (String) record.get("leaveType");
            double totalDays = record.get("totalDays") != null ? ((Number) record.get("totalDays")).doubleValue() : 0.0;
            monthlyUsageByType.computeIfAbsent(type, k -> new ArrayList<>()).add(totalDays);
        }

        for (com.company.oa.entity.oa.LeaveBalance balance : balances) {
            Map<String, Object> typeInfo = new LinkedHashMap<>();
            String leaveType = balance.getLeaveType();
            typeInfo.put("leaveType", leaveType);
            typeInfo.put("totalDays", balance.getTotalDays());
            typeInfo.put("usedDays", balance.getUsedDays());
            typeInfo.put("remainingDays", balance.getRemainingDays());

            // Calculate usage rate (days per month over last 3 months)
            List<Double> monthlyUsages = monthlyUsageByType.getOrDefault(leaveType, new ArrayList<>());
            double avgMonthlyUsage = 0.0;
            if (!monthlyUsages.isEmpty()) {
                avgMonthlyUsage = monthlyUsages.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
            }
            typeInfo.put("avgMonthlyUsage", roundToOneDecimal(avgMonthlyUsage));

            // Project depletion date
            double remaining = balance.getRemainingDays() != null ? balance.getRemainingDays() : 0.0;
            String depletionForecast = "余额充足";
            if (avgMonthlyUsage > 0 && remaining > 0) {
                int monthsUntilDepletion = (int) Math.ceil(remaining / avgMonthlyUsage);
                YearMonth depletionMonth = YearMonth.now().plusMonths(monthsUntilDepletion);
                String depletionLabel = depletionMonth.getMonthValue() + " 月";
                if (monthsUntilDepletion <= 1) {
                    depletionForecast = "将在本月耗尽";
                } else {
                    depletionForecast = leaveType + " 将在 " + depletionLabel + " 耗尽";
                }
            } else if (remaining <= 0) {
                depletionForecast = "已耗尽";
            }
            typeInfo.put("depletionForecast", depletionForecast);
            typeInfo.put("monthsUntilDepletion",
                    avgMonthlyUsage > 0 && remaining > 0
                            ? (int) Math.ceil(remaining / avgMonthlyUsage)
                            : -1);

            leaveTypes.add(typeInfo);
        }

        forecast.put("leaveTypes", leaveTypes);
        return forecast;
    }

    // ─── Contract Expiry Alert ───────────────────────────────────────────

    private Map<String, Object> buildContractExpiryAlert() {
        Map<String, Object> alert = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        // Contracts expiring in 3 days
        List<Map<String, Object>> in3Days = contractInfoMapper.selectAllExpiringContracts(
                today, today.plusDays(3));
        // Contracts expiring in 7 days (excluding those already in 3-day list)
        List<Map<String, Object>> in7Days = contractInfoMapper.selectAllExpiringContracts(
                today.plusDays(4), today.plusDays(7));
        // Contracts expiring in 30 days (excluding those already in 7-day list)
        List<Map<String, Object>> in30Days = contractInfoMapper.selectAllExpiringContracts(
                today.plusDays(8), today.plusDays(30));

        alert.put("criticalCount", in3Days != null ? in3Days.size() : 0);
        alert.put("criticalContracts", in3Days != null ? in3Days : new ArrayList<>());
        alert.put("warningCount", in7Days != null ? in7Days.size() : 0);
        alert.put("warningContracts", in7Days != null ? in7Days : new ArrayList<>());
        alert.put("infoCount", in30Days != null ? in30Days.size() : 0);
        alert.put("infoContracts", in30Days != null ? in30Days : new ArrayList<>());
        alert.put("totalCount",
                (in3Days != null ? in3Days.size() : 0)
                        + (in7Days != null ? in7Days.size() : 0)
                        + (in30Days != null ? in30Days.size() : 0));

        return alert;
    }

    // ─── Approval Workload Prediction ───────────────────────────────────

    private Map<String, Object> buildApprovalWorkloadPrediction(long userId) {
        Map<String, Object> prediction = new LinkedHashMap<>();

        // Get daily completed task counts for last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Map<String, Object>> dailyCounts = wfTaskMapper.selectDailyCompletedTaskCounts(userId, thirtyDaysAgo);

        double avgTasksPerDay = 0.0;
        if (dailyCounts != null && !dailyCounts.isEmpty()) {
            int totalTasks = dailyCounts.stream()
                    .mapToInt(m -> m.get("taskCount") != null ? ((Number) m.get("taskCount")).intValue() : 0)
                    .sum();
            avgTasksPerDay = (double) totalTasks / 30.0;
        }

        // Today's pending count
        Long pendingCount = wfTaskMapper.countTodoTasks(userId, "PENDING");
        long todayPending = pendingCount != null ? pendingCount : 0L;

        prediction.put("avgTasksPerDay", roundToOneDecimal(avgTasksPerDay));
        prediction.put("todayPendingCount", todayPending);

        // Workload comparison message
        String workloadMessage;
        if (avgTasksPerDay > 0) {
            double ratio = todayPending / avgTasksPerDay;
            int percentHigher = (int) ((ratio - 1.0) * 100);
            if (percentHigher > 0) {
                workloadMessage = "今天待办量高于平均 " + percentHigher + "%";
            } else if (percentHigher < -10) {
                workloadMessage = "今天待办量低于平均 " + Math.abs(percentHigher) + "%";
            } else {
                workloadMessage = "今天待办量与平均持平";
            }
            prediction.put("workloadRatio", roundToOneDecimal(ratio));
        } else {
            workloadMessage = "尚无历史数据，无法预测工作量";
            prediction.put("workloadRatio", null);
        }
        prediction.put("workloadMessage", workloadMessage);

        // Active working days in the 30-day window
        prediction.put("activeDays", dailyCounts != null ? dailyCounts.size() : 0);
        prediction.put("analysisWindowDays", 30);

        return prediction;
    }

    // ─── SLA Risk Count ──────────────────────────────────────────────────

    private Map<String, Object> buildSlaRiskCount(long userId) {
        Map<String, Object> risk = new LinkedHashMap<>();

        List<Map<String, Object>> slaTasks = wfTaskMapper.selectSlaTasksForUser(userId);

        int approachingDeadline = 0;  // < 4 hours remaining
        int alreadyBreached = 0;      // sla_deadline < now

        List<Map<String, Object>> approachingList = new ArrayList<>();
        List<Map<String, Object>> breachedList = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        if (slaTasks != null) {
            for (Map<String, Object> task : slaTasks) {
                Object hoursRemainingObj = task.get("hoursRemaining");
                if (hoursRemainingObj == null) continue;

                long hoursRemaining = ((Number) hoursRemainingObj).longValue();

                if (hoursRemaining < 0) {
                    // Already breached
                    alreadyBreached++;
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("taskId", task.get("taskId"));
                    entry.put("title", task.get("title"));
                    entry.put("nodeName", task.get("nodeName"));
                    entry.put("slaDeadline", task.get("slaDeadline"));
                    entry.put("hoursOverdue", Math.abs(hoursRemaining));
                    breachedList.add(entry);
                } else if (hoursRemaining < 4) {
                    // Approaching deadline (< 4 hours remaining)
                    approachingDeadline++;
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("taskId", task.get("taskId"));
                    entry.put("title", task.get("title"));
                    entry.put("nodeName", task.get("nodeName"));
                    entry.put("slaDeadline", task.get("slaDeadline"));
                    entry.put("hoursRemaining", hoursRemaining);
                    approachingList.add(entry);
                }
            }
        }

        risk.put("approachingDeadlineCount", approachingDeadline);
        risk.put("approachingDeadlineTasks", approachingList);
        risk.put("alreadyBreachedCount", alreadyBreached);
        risk.put("alreadyBreachedTasks", breachedList);
        risk.put("totalRiskCount", approachingDeadline + alreadyBreached);

        return risk;
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
