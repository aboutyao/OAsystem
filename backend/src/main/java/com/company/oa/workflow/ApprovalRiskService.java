package com.company.oa.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.oa.OaExpense;
import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.entity.wf.WfProcessInstance;
import com.company.oa.entity.wf.WfTask;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import com.company.oa.workflow.mapper.WfTaskRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates a composite risk score (0-100) for each approval request.
 *
 * <p>Scoring dimensions:
 * <ul>
 *   <li><b>Amount Risk</b> (0-30): deviation from requester's historical average</li>
 *   <li><b>Pattern Risk</b> (0-30): submission frequency, month-over-month increase, off-hours</li>
 *   <li><b>SLA Risk</b> (0-20): deadline proximity and breach status</li>
 *   <li><b>History Risk</b> (0-20): rejection rate, first-time requester, overdue items</li>
 * </ul>
 */
@Service
public class ApprovalRiskService {

    private final WfProcessInstanceMapper instanceMapper;
    private final WfTaskMapper wfTaskMapper;
    private final WfTaskRecordMapper taskRecordMapper;
    private final OaExpenseMapper expenseMapper;
    private final OaPurchaseMapper purchaseMapper;

    public ApprovalRiskService(
            WfProcessInstanceMapper instanceMapper,
            WfTaskMapper wfTaskMapper,
            WfTaskRecordMapper taskRecordMapper,
            OaExpenseMapper expenseMapper,
            OaPurchaseMapper purchaseMapper
    ) {
        this.instanceMapper = instanceMapper;
        this.wfTaskMapper = wfTaskMapper;
        this.taskRecordMapper = taskRecordMapper;
        this.expenseMapper = expenseMapper;
        this.purchaseMapper = purchaseMapper;
    }

    /**
     * Calculates a risk score for the given workflow instance.
     *
     * @param wfInstanceId the workflow instance ID
     * @return map containing {@code score}, {@code level}, and {@code factors}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateRisk(long wfInstanceId) {
        Map<String, Object> inst = instanceMapper.loadInstance(wfInstanceId);
        if (inst == null || inst.isEmpty()) {
            return defaultResult("流程实例不存在");
        }

        long starterId = ((Number) inst.get("starterId")).longValue();
        String businessType = String.valueOf(inst.get("businessType"));
        Object businessIdObj = inst.get("businessId");
        long businessId = businessIdObj != null ? ((Number) businessIdObj).longValue() : 0;

        // Extract the current amount from the business document
        BigDecimal currentAmount = extractAmount(businessType, businessId);

        List<Map<String, Object>> factors = new ArrayList<>();
        int totalScore = 0;

        // 1. Amount Risk (0-30)
        int amountScore = computeAmountRisk(starterId, currentAmount, businessType, factors);
        totalScore += amountScore;

        // 2. Pattern Risk (0-30)
        int patternScore = computePatternRisk(starterId, currentAmount, factors);
        totalScore += patternScore;

        // 3. SLA Risk (0-20)
        int slaScore = computeSlaRisk(inst, factors);
        totalScore += slaScore;

        // 4. History Risk (0-20)
        int historyScore = computeHistoryRisk(starterId, factors);
        totalScore += historyScore;

        // Clamp to 0-100
        totalScore = Math.max(0, Math.min(100, totalScore));

        String level = resolveLevel(totalScore);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", totalScore);
        result.put("level", level);
        result.put("factors", factors);
        return result;
    }

    // ─── Amount Risk (0-30) ──────────────────────────────────────────

    private int computeAmountRisk(long starterId, BigDecimal currentAmount,
                                  String businessType, List<Map<String, Object>> factors) {
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal historicalAvg = computeHistoricalAverageAmount(starterId, businessType);
        if (historicalAvg == null || historicalAvg.compareTo(BigDecimal.ZERO) <= 0) {
            // No history to compare against — no amount risk signal
            return 0;
        }

        double ratio = currentAmount.doubleValue() / historicalAvg.doubleValue();
        int score;
        String description;

        if (ratio > 3.0) {
            score = 30;
            description = String.format("金额 %.0f 为历史均值 %.0f 的 %.1f 倍(>3x)",
                    currentAmount.doubleValue(), historicalAvg.doubleValue(), ratio);
        } else if (ratio > 2.0) {
            score = 20;
            description = String.format("金额 %.0f 为历史均值 %.0f 的 %.1f 倍(>2x)",
                    currentAmount.doubleValue(), historicalAvg.doubleValue(), ratio);
        } else if (ratio > 1.5) {
            score = 10;
            description = String.format("金额 %.0f 为历史均值 %.0f 的 %.1f 倍(>1.5x)",
                    currentAmount.doubleValue(), historicalAvg.doubleValue(), ratio);
        } else {
            score = 0;
            description = String.format("金额 %.0f 在历史均值 %.0f 的正常范围内",
                    currentAmount.doubleValue(), historicalAvg.doubleValue());
        }

        if (score > 0) {
            factors.add(Map.of(
                    "dimension", "AMOUNT",
                    "score", score,
                    "maxScore", 30,
                    "description", description
            ));
        }
        return score;
    }

    /**
     * Computes the requester's historical average amount for the given business type.
     * Looks at the last 20 completed (APPROVED/REJECTED) instances of the same business type.
     */
    private BigDecimal computeHistoricalAverageAmount(long starterId, String businessType) {
        List<WfProcessInstance> history = instanceMapper.selectList(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, starterId)
                        .eq(WfProcessInstance::getBusinessType, businessType)
                        .in(WfProcessInstance::getStatus, "APPROVED", "REJECTED")
                        .orderByDesc(WfProcessInstance::getId)
                        .last("limit 20"));

        if (history.isEmpty()) return null;

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (WfProcessInstance inst : history) {
            BigDecimal amt = extractAmount(businessType, inst.getBusinessId());
            if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(amt);
                count++;
            }
        }
        return count > 0 ? total.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP) : null;
    }

    // ─── Pattern Risk (0-30) ─────────────────────────────────────────

    private int computePatternRisk(long starterId, BigDecimal currentAmount,
                                   List<Map<String, Object>> factors) {
        int score = 0;

        // Same requester submitted > 3 times this month (20 points)
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Long thisMonthCount = instanceMapper.selectCount(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, starterId)
                        .ge(WfProcessInstance::getStartedAt, monthStart));

        if (thisMonthCount != null && thisMonthCount > 3) {
            score += 20;
            factors.add(Map.of(
                    "dimension", "FREQUENCY",
                    "score", 20,
                    "maxScore", 20,
                    "description", String.format("本月已提交 %d 次申请(>3次)", thisMonthCount)
            ));
        }

        // Amount increases month-over-month (10 points)
        if (currentAmount != null && currentAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal lastMonthAvg = computeMonthlyAverageAmount(starterId, 1);
            BigDecimal twoMonthsAvg = computeMonthlyAverageAmount(starterId, 2);

            if (lastMonthAvg != null && lastMonthAvg.compareTo(BigDecimal.ZERO) > 0) {
                boolean increasing = currentAmount.compareTo(lastMonthAvg) > 0;
                if (increasing) {
                    score += 10;
                    factors.add(Map.of(
                            "dimension", "AMOUNT_TREND",
                            "score", 10,
                            "maxScore", 10,
                            "description", String.format("金额 %.0f 较上月均值 %.0f 增长",
                                    currentAmount.doubleValue(), lastMonthAvg.doubleValue())
                    ));
                }
            }
        }

        // Requesting outside normal hours (9-18) (5 points)
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(9, 0)) || now.isAfter(LocalTime.of(18, 0))) {
            score += 5;
            factors.add(Map.of(
                    "dimension", "OFF_HOURS",
                    "score", 5,
                    "maxScore", 5,
                    "description", String.format("非工作时间提交(当前 %d:%02d)", now.getHour(), now.getMinute())
            ));
        }

        return Math.min(30, score);
    }

    private BigDecimal computeMonthlyAverageAmount(long starterId, int monthsAgo) {
        LocalDateTime start = LocalDateTime.now().minusMonths(monthsAgo).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = start.plusMonths(1).minusNanos(1);

        List<WfProcessInstance> monthInstances = instanceMapper.selectList(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, starterId)
                        .between(WfProcessInstance::getStartedAt, start, end)
                        .in(WfProcessInstance::getStatus, "APPROVED", "REJECTED")
                        .last("limit 20"));

        if (monthInstances.isEmpty()) return null;

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (WfProcessInstance inst : monthInstances) {
            BigDecimal amt = extractAmount(inst.getBusinessType(), inst.getBusinessId());
            if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(amt);
                count++;
            }
        }
        return count > 0 ? total.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP) : null;
    }

    // ─── SLA Risk (0-20) ─────────────────────────────────────────────

    private int computeSlaRisk(Map<String, Object> inst, List<Map<String, Object>> factors) {
        Object slaDeadlineObj = inst.get("slaDeadline");
        Object statusObj = inst.get("status");

        if (slaDeadlineObj == null || !"APPROVING".equals(String.valueOf(statusObj))) {
            return 0;
        }

        LocalDateTime slaDeadline = null;
        if (slaDeadlineObj instanceof LocalDateTime ldt) {
            slaDeadline = ldt;
        } else {
            try {
                slaDeadline = LocalDateTime.parse(String.valueOf(slaDeadlineObj));
            } catch (Exception ignore) {
                return 0;
            }
        }

        long hoursRemaining = ChronoUnit.HOURS.between(LocalDateTime.now(), slaDeadline);
        int score;

        if (hoursRemaining < 0) {
            // Already breached
            score = 20;
            factors.add(Map.of(
                    "dimension", "SLA_BREACH",
                    "score", 20,
                    "maxScore", 20,
                    "description", String.format("SLA 已超期 %d 小时", Math.abs(hoursRemaining))
            ));
        } else if (hoursRemaining < 4) {
            score = 15;
            factors.add(Map.of(
                    "dimension", "SLA_CRITICAL",
                    "score", 15,
                    "maxScore", 20,
                    "description", String.format("SLA 剩余不足 %d 小时", hoursRemaining)
            ));
        } else if (hoursRemaining < 12) {
            score = 10;
            factors.add(Map.of(
                    "dimension", "SLA_WARNING",
                    "score", 10,
                    "maxScore", 20,
                    "description", String.format("SLA 剩余 %d 小时", hoursRemaining)
            ));
        } else {
            score = 0;
        }

        return score;
    }

    // ─── History Risk (0-20) ─────────────────────────────────────────

    private int computeHistoryRisk(long starterId, List<Map<String, Object>> factors) {
        int score = 0;

        // Requester's past submissions were frequently rejected (20 points)
        Long totalSubmissions = instanceMapper.selectCount(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, starterId)
                        .in(WfProcessInstance::getStatus, "APPROVED", "REJECTED"));

        Long rejectedCount = instanceMapper.selectCount(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, starterId)
                        .eq(WfProcessInstance::getStatus, "REJECTED"));

        if (totalSubmissions != null && totalSubmissions >= 3) {
            double rejectRate = (double) rejectedCount / totalSubmissions;
            if (rejectRate >= 0.5) {
                score += 20;
                factors.add(Map.of(
                        "dimension", "REJECT_RATE",
                        "score", 20,
                        "maxScore", 20,
                        "description", String.format("历史驳回率 %.0f%% (%d/%d)",
                                rejectRate * 100, rejectedCount, totalSubmissions)
                ));
            }
        }

        // First-time requester (10 points)
        if (totalSubmissions == null || totalSubmissions == 0) {
            score += 10;
            factors.add(Map.of(
                    "dimension", "FIRST_TIME",
                    "score", 10,
                    "maxScore", 20,
                    "description", "首次提交申请"
            ));
        }

        // Requester has pending overdue items (10 points)
        Long overdueCount = instanceMapper.countExceptionByStarter(starterId);
        if (overdueCount != null && overdueCount > 0) {
            score += 10;
            factors.add(Map.of(
                    "dimension", "PENDING_OVERDUE",
                    "score", 10,
                    "maxScore", 20,
                    "description", String.format("有 %d 个待处理/逾期流程", overdueCount)
            ));
        }

        return Math.min(20, score);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private BigDecimal extractAmount(String businessType, long businessId) {
        if (businessId <= 0) return null;

        return switch (businessType) {
            case "EXPENSE", "EXPENSE_HIGH" -> {
                OaExpense expense = expenseMapper.selectById(businessId);
                yield expense != null ? expense.getTotalAmount() : null;
            }
            case "PURCHASE" -> {
                OaPurchase purchase = purchaseMapper.selectById(businessId);
                yield purchase != null ? purchase.getTotalAmount() : null;
            }
            default -> null;
        };
    }

    private String resolveLevel(int score) {
        if (score >= 70) return "CRITICAL";
        if (score >= 45) return "HIGH";
        if (score >= 20) return "MEDIUM";
        return "LOW";
    }

    private Map<String, Object> defaultResult(String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", 0);
        result.put("level", "LOW");
        result.put("factors", List.of(Map.of(
                "dimension", "UNKNOWN",
                "score", 0,
                "maxScore", 0,
                "description", reason
        )));
        return result;
    }
}
