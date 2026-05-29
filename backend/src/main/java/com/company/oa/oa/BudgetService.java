package com.company.oa.oa;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.oa.Budget;
import com.company.oa.message.MessageService;
import com.company.oa.oa.mapper.BudgetMapper;
import com.company.oa.org.mapper.DeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {
    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
    private final BudgetMapper budgetMapper;
    private final AuthService authService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final MessageService messageService;
    private final DeptMapper deptMapper;

    public BudgetService(BudgetMapper budgetMapper, AuthService authService,
                        AuditService auditService, SequenceService sequenceService,
                        MessageService messageService, DeptMapper deptMapper) {
        this.budgetMapper = budgetMapper;
        this.authService = authService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.messageService = messageService;
        this.deptMapper = deptMapper;
    }

    @Transactional
    public Map<String, Object> createBudget(Budget budget) {
        AuthUser user = authService.currentUser();
        budget.setId(sequenceService.nextId("oa_budget"));
        budget.setCreatedBy(user.id());
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setStatus("ACTIVE");
        budget.setDeleted(0);
        budget.setCreatedAt(LocalDateTime.now());
        budgetMapper.insert(budget);
        auditService.safeRecordOperation(user.id(), "BUDGET_CREATE", "BUDGET", budget.getId(), AuditService.SUCCESS, null);
        return Map.of("id", budget.getId(), "status", "CREATED");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBudgets(Long deptId, String category, Integer year) {
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getStatus, "ACTIVE")
                .eq(Budget::getDeleted, 0);

        if (deptId != null) {
            wrapper.eq(Budget::getDeptId, deptId);
        }
        if (category != null) {
            wrapper.eq(Budget::getCategory, category);
        }
        if (year != null) {
            wrapper.eq(Budget::getYear, year);
        }

        List<Budget> budgets = budgetMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Budget b : budgets) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", b.getId());
            item.put("deptId", b.getDeptId());
            item.put("budgetType", b.getBudgetType());
            item.put("year", b.getYear());
            item.put("month", b.getMonth());
            item.put("quarter", b.getQuarter());
            item.put("category", b.getCategory());
            item.put("budgetAmount", b.getBudgetAmount());
            item.put("usedAmount", b.getUsedAmount());
            item.put("remainingAmount", b.getBudgetAmount().subtract(b.getUsedAmount()));
            item.put("usagePercent", b.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? b.getUsedAmount().multiply(BigDecimal.valueOf(100))
                            .divide(b.getBudgetAmount(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            item.put("warningThreshold", b.getWarningThreshold());
            item.put("isOverBudget", b.getUsedAmount().compareTo(b.getBudgetAmount()) > 0);
            item.put("isNearLimit", b.getUsedAmount().multiply(BigDecimal.valueOf(100))
                    .divide(b.getBudgetAmount(), 1, RoundingMode.HALF_UP)
                    .compareTo(b.getWarningThreshold()) >= 0);
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void recordUsage(Long deptId, String category, BigDecimal amount, String description) {
        int year = java.time.Year.now().getValue();
        int month = java.time.LocalDate.now().getMonthValue();

        // 查找月度预算
        Budget budget = budgetMapper.selectOne(
                new LambdaQueryWrapper<Budget>()
                        .eq(Budget::getDeptId, deptId)
                        .eq(Budget::getCategory, category)
                        .eq(Budget::getBudgetType, "MONTHLY")
                        .eq(Budget::getYear, year)
                        .eq(Budget::getMonth, month)
                        .eq(Budget::getStatus, "ACTIVE")
                        .eq(Budget::getDeleted, 0)
        );

        if (budget == null) {
            log.warn("未找到部门 {} 的 {} 类型预算", deptId, category);
            return;
        }

        budget.setUsedAmount(budget.getUsedAmount().add(amount));
        budget.setUpdatedAt(LocalDateTime.now());
        budgetMapper.updateById(budget);

        // 检查是否超支
        checkBudgetWarning(budget, description);
    }

    private void checkBudgetWarning(Budget budget, String description) {
        BigDecimal usagePercent = budget.getUsedAmount().multiply(BigDecimal.valueOf(100))
                .divide(budget.getBudgetAmount(), 1, RoundingMode.HALF_UP);

        if (budget.getUsedAmount().compareTo(budget.getBudgetAmount()) > 0) {
            // 超支告警
            log.warn("预算超支: deptId={}, category={}, used={}, budget={}",
                    budget.getDeptId(), budget.getCategory(),
                    budget.getUsedAmount(), budget.getBudgetAmount());
            sendBudgetAlert(budget, "OVER_BUDGET", usagePercent, description);
        } else if (usagePercent.compareTo(budget.getWarningThreshold()) >= 0) {
            // 接近上限告警
            log.warn("预算接近上限: deptId={}, category={}, usage={}%, threshold={}%",
                    budget.getDeptId(), budget.getCategory(),
                    usagePercent, budget.getWarningThreshold());
            sendBudgetAlert(budget, "NEAR_LIMIT", usagePercent, description);
        }
    }

    private void sendBudgetAlert(Budget budget, String alertType, BigDecimal usagePercent, String description) {
        // 通知财务管理员和部门负责人
        String message = switch (alertType) {
            case "OVER_BUDGET" -> String.format("【预算超支告警】部门 %s 的 %s 预算已超支，使用率 %s%%，请立即处理。",
                    budget.getDeptId(), budget.getCategory(), usagePercent);
            case "NEAR_LIMIT" -> String.format("【预算预警】部门 %s 的 %s 预算使用率已达 %s%%，接近预警阈值 %s%%。",
                    budget.getDeptId(), budget.getCategory(), usagePercent, budget.getWarningThreshold());
            default -> "";
        };

        // 这里可以发送给财务管理员和部门负责人
        // 暂时记录日志
        log.info("Budget alert: {}", message);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBudgetWarnings() {
        List<Map<String, Object>> warnings = new ArrayList<>();

        // 获取接近上限的预算
        List<Map<String, Object>> nearLimit = budgetMapper.selectBudgetsNearLimit();
        for (Map<String, Object> item : nearLimit) {
            BigDecimal budgetAmount = (BigDecimal) item.get("budgetAmount");
            BigDecimal usedAmount = (BigDecimal) item.get("usedAmount");
            BigDecimal usagePercent = usedAmount.multiply(BigDecimal.valueOf(100))
                    .divide(budgetAmount, 1, RoundingMode.HALF_UP);

            Map<String, Object> warning = new LinkedHashMap<>();
            warning.put("budgetId", item.get("id"));
            warning.put("deptName", item.get("deptName"));
            warning.put("category", item.get("category"));
            warning.put("budgetAmount", budgetAmount);
            warning.put("usedAmount", usedAmount);
            warning.put("usagePercent", usagePercent);
            warning.put("alertType", "NEAR_LIMIT");
            warning.put("message", String.format("%s 的 %s 预算使用率已达 %s%%",
                    item.get("deptName"), item.get("category"), usagePercent));
            warnings.add(warning);
        }

        // 获取已超支的预算
        List<Map<String, Object>> overBudget = budgetMapper.selectOverBudgets();
        for (Map<String, Object> item : overBudget) {
            BigDecimal budgetAmount = (BigDecimal) item.get("budgetAmount");
            BigDecimal usedAmount = (BigDecimal) item.get("usedAmount");
            BigDecimal overspend = usedAmount.subtract(budgetAmount);

            Map<String, Object> warning = new LinkedHashMap<>();
            warning.put("budgetId", item.get("id"));
            warning.put("deptName", item.get("deptName"));
            warning.put("category", item.get("category"));
            warning.put("budgetAmount", budgetAmount);
            warning.put("usedAmount", usedAmount);
            warning.put("overspend", overspend);
            warning.put("alertType", "OVER_BUDGET");
            warning.put("message", String.format("%s 的 %s 预算已超支 %s 元",
                    item.get("deptName"), item.get("category"), overspend));
            warnings.add(warning);
        }

        return warnings;
    }
}
