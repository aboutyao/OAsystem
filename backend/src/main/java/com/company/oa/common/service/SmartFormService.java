package com.company.oa.common.service;

import com.company.oa.entity.oa.OaExpense;
import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能表单服务
 * 基于历史数据自动填充表单
 */
@Service
public class SmartFormService {
    private final OaExpenseMapper expenseMapper;
    private final OaPurchaseMapper purchaseMapper;

    public SmartFormService(OaExpenseMapper expenseMapper, OaPurchaseMapper purchaseMapper) {
        this.expenseMapper = expenseMapper;
        this.purchaseMapper = purchaseMapper;
    }

    /**
     * 获取供应商智能推荐
     */
    public List<SupplierSuggestion> getSupplierSuggestions(long userId, String keyword) {
        // 从历史采购单中提取供应商信息
        List<OaPurchase> history = purchaseMapper.selectList(
            new LambdaQueryWrapper<OaPurchase>()
                .eq(OaPurchase::getCreatedBy, userId)
                .isNotNull(OaPurchase::getSupplierName)
                .orderByDesc(OaPurchase::getCreatedAt)
                .last("LIMIT 20")
        );

        // 按供应商名聚合，统计使用次数
        Map<String, Long> supplierCount = history.stream()
            .filter(p -> p.getSupplierName() != null)
            .collect(Collectors.groupingBy(OaPurchase::getSupplierName, Collectors.counting()));

        // 过滤并排序
        return supplierCount.entrySet().stream()
            .filter(e -> keyword == null || keyword.isEmpty() || e.getKey().contains(keyword))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> new SupplierSuggestion(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * 获取费用科目智能推荐
     */
    public List<String> getExpenseCategorySuggestions(long userId, String keyword) {
        List<OaExpense> history = expenseMapper.selectList(
            new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getCreatedBy, userId)
                .isNotNull(OaExpense::getExpenseType)
                .orderByDesc(OaExpense::getCreatedAt)
                .last("LIMIT 50")
        );

        Map<String, Long> categoryCount = history.stream()
            .filter(e -> e.getExpenseType() != null)
            .collect(Collectors.groupingBy(OaExpense::getExpenseType, Collectors.counting()));

        return categoryCount.entrySet().stream()
            .filter(e -> keyword == null || keyword.isEmpty() || e.getKey().contains(keyword))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 获取历史报销模板
     */
    public List<ExpenseTemplate> getExpenseTemplates(long userId) {
        List<OaExpense> recentExpenses = expenseMapper.selectList(
            new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getCreatedBy, userId)
                .orderByDesc(OaExpense::getCreatedAt)
                .last("LIMIT 10")
        );

        return recentExpenses.stream()
            .map(e -> new ExpenseTemplate(
                e.getId(),
                e.getReason(),
                e.getExpenseType(),
                e.getTotalAmount(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    public static class SupplierSuggestion {
        private final String name;
        private final long usageCount;

        public SupplierSuggestion(String name, long usageCount) {
            this.name = name;
            this.usageCount = usageCount;
        }

        public String getName() { return name; }
        public long getUsageCount() { return usageCount; }
    }

    public static class ExpenseTemplate {
        private final Long id;
        private final String title;
        private final String category;
        private final java.math.BigDecimal amount;
        private final java.time.LocalDateTime createdAt;

        public ExpenseTemplate(Long id, String title, String category, java.math.BigDecimal amount, java.time.LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.amount = amount;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getExpenseType() { return category; }
        public java.math.BigDecimal getAmount() { return amount; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    }
}
