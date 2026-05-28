package com.company.oa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.oa.OaExpense;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 异常检测服务
 * 检测异常请假、异常报销等
 */
@Service
public class AnomalyDetectionService {
    private final OaLeaveMapper leaveMapper;
    private final OaExpenseMapper expenseMapper;

    public AnomalyDetectionService(OaLeaveMapper leaveMapper, OaExpenseMapper expenseMapper) {
        this.leaveMapper = leaveMapper;
        this.expenseMapper = expenseMapper;
    }

    public List<Map<String, Object>> detectAnomalies() {
        List<Map<String, Object>> anomalies = new ArrayList<>();
        anomalies.addAll(detectFrequentLeave());
        anomalies.addAll(detectHighExpense());
        return anomalies;
    }

    private List<Map<String, Object>> detectFrequentLeave() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);

        List<OaLeave> recentLeaves = leaveMapper.selectList(
                new LambdaQueryWrapper<OaLeave>()
                        .eq(OaLeave::getDeleted, 0)
                        .ge(OaLeave::getCreatedAt, oneMonthAgo)
        );

        Map<Long, Long> userLeaveCount = new HashMap<>();
        for (OaLeave leave : recentLeaves) {
            userLeaveCount.merge(leave.getCreatedBy(), 1L, Long::sum);
        }

        if (userLeaveCount.isEmpty()) return result;

        double avgCount = userLeaveCount.values().stream().mapToLong(Long::longValue).average().orElse(0);
        double threshold = avgCount * 2;

        for (Map.Entry<Long, Long> entry : userLeaveCount.entrySet()) {
            if (entry.getValue() > threshold && entry.getValue() > 3) {
                Map<String, Object> anomaly = new LinkedHashMap<>();
                anomaly.put("type", "FREQUENT_LEAVE");
                anomaly.put("userId", entry.getKey());
                anomaly.put("count", entry.getValue());
                anomaly.put("threshold", (long) threshold);
                anomaly.put("message", "该员工近30天请假" + entry.getValue() + "次，超过平均值的2倍");
                anomaly.put("severity", entry.getValue() > threshold * 2 ? "HIGH" : "MEDIUM");
                result.add(anomaly);
            }
        }
        return result;
    }

    private List<Map<String, Object>> detectHighExpense() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);

        List<OaExpense> recentExpenses = expenseMapper.selectList(
                new LambdaQueryWrapper<OaExpense>()
                        .eq(OaExpense::getDeleted, 0)
                        .ge(OaExpense::getCreatedAt, oneMonthAgo)
                        .eq(OaExpense::getStatus, "APPROVED")
        );

        if (recentExpenses.isEmpty()) return result;

        BigDecimal totalAmount = recentExpenses.stream()
                .map(OaExpense::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgAmount = totalAmount.divide(BigDecimal.valueOf(recentExpenses.size()), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(3));

        for (OaExpense expense : recentExpenses) {
            if (expense.getTotalAmount() != null && expense.getTotalAmount().compareTo(threshold) > 0) {
                Map<String, Object> anomaly = new LinkedHashMap<>();
                anomaly.put("type", "HIGH_EXPENSE");
                anomaly.put("userId", expense.getCreatedBy());
                anomaly.put("expenseId", expense.getId());
                anomaly.put("amount", expense.getTotalAmount());
                anomaly.put("avgAmount", avgAmount);
                anomaly.put("message", "报销金额" + expense.getTotalAmount() + "元，超过平均值的3倍");
                anomaly.put("severity", expense.getTotalAmount().compareTo(threshold.multiply(BigDecimal.valueOf(2))) > 0 ? "HIGH" : "MEDIUM");
                result.add(anomaly);
            }
        }
        return result;
    }
}
