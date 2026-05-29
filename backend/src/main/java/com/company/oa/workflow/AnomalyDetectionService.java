package com.company.oa.workflow;

import com.company.oa.audit.AuditService;
import com.company.oa.entity.oa.OaExpense;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.entity.oa.OaSealApply;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.oa.mapper.OaSealApplyMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 异常行为检测服务
 * 检测异常模式：深夜提交、频繁修改、大额异常等
 */
@Service
public class AnomalyDetectionService {
    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final OaExpenseMapper expenseMapper;
    private final OaLeaveMapper leaveMapper;
    private final OaPurchaseMapper purchaseMapper;
    private final OaSealApplyMapper sealApplyMapper;
    private final WfTaskMapper wfTaskMapper;
    private final AuditService auditService;

    public AnomalyDetectionService(OaExpenseMapper expenseMapper, OaLeaveMapper leaveMapper,
                                    OaPurchaseMapper purchaseMapper, OaSealApplyMapper sealApplyMapper,
                                    WfTaskMapper wfTaskMapper, AuditService auditService) {
        this.expenseMapper = expenseMapper;
        this.leaveMapper = leaveMapper;
        this.purchaseMapper = purchaseMapper;
        this.sealApplyMapper = sealApplyMapper;
        this.wfTaskMapper = wfTaskMapper;
        this.auditService = auditService;
    }

    /**
     * 检测用户异常行为
     */
    public List<AnomalyRecord> detectAnomalies(long userId) {
        List<AnomalyRecord> anomalies = new ArrayList<>();

        // 1. 检测深夜提交
        anomalies.addAll(detectLateNightSubmissions(userId));

        // 2. 检测频繁修改
        anomalies.addAll(detectFrequentModifications(userId));

        // 3. 检测大额异常
        anomalies.addAll(detectLargeAmountAnomalies(userId));

        // 4. 检测重复提交
        anomalies.addAll(detectDuplicateSubmissions(userId));

        // 记录异常检测结果
        if (!anomalies.isEmpty()) {
            log.warn("用户 {} 检测到 {} 条异常行为", userId, anomalies.size());
        }

        return anomalies;
    }

    private List<AnomalyRecord> detectLateNightSubmissions(long userId) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        LocalDateTime nightStart = LocalDate.now().minusDays(7).atTime(LocalTime.of(22, 0));
        LocalDateTime nightEnd = LocalDate.now().atTime(LocalTime.of(6, 0));

        // 检测报销单深夜提交
        List<OaExpense> expenses = expenseMapper.selectList(
            new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getCreatedBy, userId)
                .ge(OaExpense::getCreatedAt, nightStart)
        );

        for (OaExpense expense : expenses) {
            LocalTime submitTime = expense.getCreatedAt().toLocalTime();
            if (submitTime.isAfter(LocalTime.of(22, 0)) || submitTime.isBefore(LocalTime.of(6, 0))) {
                anomalies.add(new AnomalyRecord(
                    "LATE_NIGHT_SUBMISSION",
                    "深夜提交",
                    "报销单 " + expense.getId() + " 在 " + submitTime.getHour() + " 点提交",
                    "HIGH",
                    expense.getId()
                ));
            }
        }

        return anomalies;
    }

    private List<AnomalyRecord> detectFrequentModifications(long userId) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        LocalDateTime weekAgo = LocalDate.now().minusDays(7).atStartOfDay();

        // 检测一周内多次修改同一单据
        List<OaExpense> expenses = expenseMapper.selectList(
            new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getCreatedBy, userId)
                .ge(OaExpense::getUpdatedAt, weekAgo)
        );

        Map<Long, Long> modificationCount = new HashMap<>();
        for (OaExpense expense : expenses) {
            modificationCount.merge(expense.getId(), 1L, Long::sum);
        }

        for (Map.Entry<Long, Long> entry : modificationCount.entrySet()) {
            if (entry.getValue() > 3) {
                anomalies.add(new AnomalyRecord(
                    "FREQUENT_MODIFICATION",
                    "频繁修改",
                    "报销单 " + entry.getKey() + " 一周内修改了 " + entry.getValue() + " 次",
                    "MEDIUM",
                    entry.getKey()
                ));
            }
        }

        return anomalies;
    }

    private List<AnomalyRecord> detectLargeAmountAnomalies(long userId) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        LocalDateTime monthAgo = LocalDate.now().minusDays(30).atStartOfDay();

        // 检测大额报销
        List<OaExpense> expenses = expenseMapper.selectList(
            new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getCreatedBy, userId)
                .ge(OaExpense::getCreatedAt, monthAgo)
        );

        BigDecimal totalAmount = BigDecimal.ZERO;
        int count = 0;
        for (OaExpense expense : expenses) {
            if (expense.getTotalAmount() != null) {
                totalAmount = totalAmount.add(expense.getTotalAmount());
                count++;
            }
        }

        // 月度总额超过10万预警
        if (totalAmount.compareTo(new BigDecimal("100000")) > 0) {
            anomalies.add(new AnomalyRecord(
                "LARGE_MONTHLY_AMOUNT",
                "月度大额",
                "本月报销总额 " + totalAmount + " 元，共 " + count + " 笔",
                "HIGH",
                null
            ));
        }

        // 单笔超过5万预警
        for (OaExpense expense : expenses) {
            if (expense.getTotalAmount() != null && expense.getTotalAmount().compareTo(new BigDecimal("50000")) > 0) {
                anomalies.add(new AnomalyRecord(
                    "LARGE_SINGLE_AMOUNT",
                    "单笔大额",
                    "报销单 " + expense.getId() + " 金额 " + expense.getTotalAmount() + " 元",
                    "MEDIUM",
                    expense.getId()
                ));
            }
        }

        return anomalies;
    }

    private List<AnomalyRecord> detectDuplicateSubmissions(long userId) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        LocalDateTime today = LocalDate.now().atStartOfDay();

        // 检测同一天多次提交
        List<OaExpense> todayExpenses = expenseMapper.selectList(
            new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getCreatedBy, userId)
                .ge(OaExpense::getCreatedAt, today)
        );

        if (todayExpenses.size() > 3) {
            anomalies.add(new AnomalyRecord(
                "DUPLICATE_SUBMISSION",
                "频繁提交",
                "今天已提交 " + todayExpenses.size() + " 笔报销单",
                "LOW",
                null
            ));
        }

        return anomalies;
    }

    /**
     * 异常记录
     */
    public static class AnomalyRecord {
        private final String type;
        private final String typeName;
        private final String description;
        private final String severity;
        private final Long relatedId;

        public AnomalyRecord(String type, String typeName, String description, String severity, Long relatedId) {
            this.type = type;
            this.typeName = typeName;
            this.description = description;
            this.severity = severity;
            this.relatedId = relatedId;
        }

        public String getType() { return type; }
        public String getTypeName() { return typeName; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public Long getRelatedId() { return relatedId; }
    }
}
