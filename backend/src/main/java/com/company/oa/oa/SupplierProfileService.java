package com.company.oa.oa;

import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供应商画像服务
 * 自动聚合供应商历史数据
 */
@Service
public class SupplierProfileService {
    private final OaPurchaseMapper purchaseMapper;

    public SupplierProfileService(OaPurchaseMapper purchaseMapper) {
        this.purchaseMapper = purchaseMapper;
    }

    /**
     * 获取供应商画像
     */
    public SupplierProfile getSupplierProfile(String supplierName) {
        List<OaPurchase> purchases = purchaseMapper.selectList(
            new LambdaQueryWrapper<OaPurchase>()
                .eq(OaPurchase::getSupplierName, supplierName)
                .orderByDesc(OaPurchase::getCreatedAt)
        );

        if (purchases.isEmpty()) {
            return null;
        }

        SupplierProfile profile = new SupplierProfile();
        profile.setSupplierName(supplierName);
        profile.setTotalOrders(purchases.size());

        // 总金额
        BigDecimal totalAmount = purchases.stream()
            .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        profile.setTotalAmount(totalAmount);

        // 平均订单金额
        profile.setAvgOrderAmount(totalAmount.divide(BigDecimal.valueOf(purchases.size()), 2, BigDecimal.ROUND_HALF_UP));

        // 最近订单时间
        profile.setLastOrderDate(purchases.get(0).getCreatedAt());

        // 订单频率（月均）
        if (purchases.size() > 1) {
            LocalDateTime firstDate = purchases.get(purchases.size() - 1).getCreatedAt();
            LocalDateTime lastDate = purchases.get(0).getCreatedAt();
            long months = java.time.temporal.ChronoUnit.MONTHS.between(firstDate, lastDate);
            if (months > 0) {
                profile.setMonthlyOrderFrequency((double) purchases.size() / months);
            }
        }

        // 活跃度评分（基于最近订单时间）
        long daysSinceLastOrder = java.time.temporal.ChronoUnit.DAYS.between(purchases.get(0).getCreatedAt(), LocalDateTime.now());
        if (daysSinceLastOrder < 30) {
            profile.setActivityLevel("HIGH");
        } else if (daysSinceLastOrder < 90) {
            profile.setActivityLevel("MEDIUM");
        } else {
            profile.setActivityLevel("LOW");
        }

        return profile;
    }

    /**
     * 获取供应商列表（带画像摘要）
     */
    public List<SupplierSummary> getSupplierSummaries() {
        List<OaPurchase> allPurchases = purchaseMapper.selectList(
            new LambdaQueryWrapper<OaPurchase>()
                .isNotNull(OaPurchase::getSupplierName)
                .orderByDesc(OaPurchase::getCreatedAt)
        );

        Map<String, List<OaPurchase>> grouped = allPurchases.stream()
            .filter(p -> p.getSupplierName() != null)
            .collect(Collectors.groupingBy(OaPurchase::getSupplierName));

        List<SupplierSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<OaPurchase>> entry : grouped.entrySet()) {
            SupplierSummary summary = new SupplierSummary();
            summary.setSupplierName(entry.getKey());
            summary.setTotalOrders(entry.getValue().size());

            BigDecimal totalAmount = entry.getValue().stream()
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalAmount(totalAmount);
            summary.setLastOrderDate(entry.getValue().get(0).getCreatedAt());

            summaries.add(summary);
        }

        return summaries;
    }

    public static class SupplierProfile {
        private String supplierName;
        private int totalOrders;
        private BigDecimal totalAmount;
        private BigDecimal avgOrderAmount;
        private LocalDateTime lastOrderDate;
        private Double monthlyOrderFrequency;
        private String activityLevel;

        // Getters and setters
        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public int getTotalOrders() { return totalOrders; }
        public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getAvgOrderAmount() { return avgOrderAmount; }
        public void setAvgOrderAmount(BigDecimal avgOrderAmount) { this.avgOrderAmount = avgOrderAmount; }
        public LocalDateTime getLastOrderDate() { return lastOrderDate; }
        public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }
        public Double getMonthlyOrderFrequency() { return monthlyOrderFrequency; }
        public void setMonthlyOrderFrequency(Double monthlyOrderFrequency) { this.monthlyOrderFrequency = monthlyOrderFrequency; }
        public String getActivityLevel() { return activityLevel; }
        public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    }

    public static class SupplierSummary {
        private String supplierName;
        private int totalOrders;
        private BigDecimal totalAmount;
        private LocalDateTime lastOrderDate;

        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public int getTotalOrders() { return totalOrders; }
        public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public LocalDateTime getLastOrderDate() { return lastOrderDate; }
        public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }
    }
}
