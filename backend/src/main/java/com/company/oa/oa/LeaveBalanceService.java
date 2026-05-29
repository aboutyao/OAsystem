package com.company.oa.oa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.entity.oa.LeaveBalance;
import com.company.oa.entity.oa.LeaveBalanceLog;
import com.company.oa.entity.oa.LeaveType;
import com.company.oa.oa.mapper.LeaveBalanceLogMapper;
import com.company.oa.oa.mapper.LeaveBalanceMapper;
import com.company.oa.oa.mapper.LeaveTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaveBalanceService {
    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveBalanceMapper balanceMapper;
    private final LeaveBalanceLogMapper balanceLogMapper;
    private final com.company.oa.oa.mapper.OaLeaveMapper leaveMapper;
    private final AuthService authService;
    private final PaginationHelper paginationHelper;

    public LeaveBalanceService(LeaveTypeMapper leaveTypeMapper, LeaveBalanceMapper balanceMapper,
                               LeaveBalanceLogMapper balanceLogMapper,
                               com.company.oa.oa.mapper.OaLeaveMapper leaveMapper,
                               AuthService authService,
                               PaginationHelper paginationHelper) {
        this.leaveTypeMapper = leaveTypeMapper;
        this.balanceMapper = balanceMapper;
        this.balanceLogMapper = balanceLogMapper;
        this.leaveMapper = leaveMapper;
        this.authService = authService;
        this.paginationHelper = paginationHelper;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyBalance() {
        AuthUser user = authService.currentUser();
        int year = java.time.Year.now().getValue();
        List<LeaveType> types = leaveTypeMapper.selectList(
            new LambdaQueryWrapper<LeaveType>().eq(LeaveType::getStatus, "ENABLED").eq(LeaveType::getDeleted, 0).orderByAsc(LeaveType::getSortOrder));

        List<LeaveBalance> balances = balanceMapper.selectList(
            new LambdaQueryWrapper<LeaveBalance>().eq(LeaveBalance::getUserId, user.id())
                .eq(LeaveBalance::getYear, year).eq(LeaveBalance::getDeleted, 0));

        Map<String, LeaveBalance> balanceByType = new LinkedHashMap<>();
        for (LeaveBalance b : balances) balanceByType.put(b.getLeaveType(), b);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (LeaveType t : types) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("typeCode", t.getTypeCode());
            item.put("typeName", t.getTypeName());
            item.put("isPaid", t.getIsPaid());
            item.put("requiresProof", t.getRequiresProof());
            LeaveBalance b = balanceByType.get(t.getTypeCode());
            item.put("totalDays", b != null ? b.getTotalDays() : t.getDaysPerYear());
            item.put("usedDays", b != null ? b.getUsedDays() : 0);
            item.put("pendingDays", b != null ? b.getPendingDays() : 0);
            item.put("remainingDays", b != null ? b.getRemainingDays() : (b != null ? b.getTotalDays() : t.getDaysPerYear()));
            result.add(item);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserBalance(long userId) {
        int year = java.time.Year.now().getValue();
        List<LeaveType> types = leaveTypeMapper.selectList(
            new LambdaQueryWrapper<LeaveType>().eq(LeaveType::getStatus, "ENABLED").eq(LeaveType::getDeleted, 0).orderByAsc(LeaveType::getSortOrder));
        List<LeaveBalance> balances = balanceMapper.selectList(
            new LambdaQueryWrapper<LeaveBalance>().eq(LeaveBalance::getUserId, userId)
                .eq(LeaveBalance::getYear, year).eq(LeaveBalance::getDeleted, 0));
        Map<String, LeaveBalance> balanceByType = new LinkedHashMap<>();
        for (LeaveBalance b : balances) balanceByType.put(b.getLeaveType(), b);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (LeaveType t : types) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("typeCode", t.getTypeCode());
            item.put("typeName", t.getTypeName());
            LeaveBalance b = balanceByType.get(t.getTypeCode());
            item.put("totalDays", b != null ? b.getTotalDays() : t.getDaysPerYear());
            item.put("usedDays", b != null ? b.getUsedDays() : 0);
            item.put("pendingDays", b != null ? b.getPendingDays() : 0);
            item.put("remainingDays", b != null ? b.getRemainingDays() : (b != null ? b.getTotalDays() : t.getDaysPerYear()));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void deductBalance(long userId, String leaveType, double days, Long leaveId) {
        int year = java.time.Year.now().getValue();
        LeaveBalance balance = balanceMapper.selectOne(
            new LambdaQueryWrapper<LeaveBalance>().eq(LeaveBalance::getUserId, userId)
                .eq(LeaveBalance::getLeaveType, leaveType).eq(LeaveBalance::getYear, year)
                .eq(LeaveBalance::getDeleted, 0));
        if (balance == null) {
            balance = new LeaveBalance();
            balance.setUserId(userId);
            balance.setLeaveType(leaveType);
            balance.setYear(year);
            balance.setTotalDays(0.0);
            balance.setUsedDays(0.0);
            balance.setPendingDays(days);
            balance.setCarriedOverDays(0.0);
            balance.setDeleted(0);
            balanceMapper.insert(balance);
        } else {
            balance.setPendingDays(balance.getPendingDays() + days);
            balance.setUpdatedAt(LocalDateTime.now());
            balanceMapper.updateById(balance);
        }
        logChange(userId, leaveType, year, "USE", -days, leaveId, "请假审批中扣减");
    }

    @Transactional
    public void confirmUsage(long userId, String leaveType, double days, Long leaveId) {
        int year = java.time.Year.now().getValue();
        LeaveBalance balance = balanceMapper.selectOne(
            new LambdaQueryWrapper<LeaveBalance>().eq(LeaveBalance::getUserId, userId)
                .eq(LeaveBalance::getLeaveType, leaveType).eq(LeaveBalance::getYear, year)
                .eq(LeaveBalance::getDeleted, 0));
        if (balance != null) {
            balance.setPendingDays(Math.max(0, balance.getPendingDays() - days));
            balance.setUsedDays(balance.getUsedDays() + days);
            balance.setUpdatedAt(LocalDateTime.now());
            balanceMapper.updateById(balance);
        }
    }

    @Transactional
    public void cancelDeduction(long userId, String leaveType, double days, Long leaveId) {
        int year = java.time.Year.now().getValue();
        LeaveBalance balance = balanceMapper.selectOne(
            new LambdaQueryWrapper<LeaveBalance>().eq(LeaveBalance::getUserId, userId)
                .eq(LeaveBalance::getLeaveType, leaveType).eq(LeaveBalance::getYear, year)
                .eq(LeaveBalance::getDeleted, 0));
        if (balance != null) {
            balance.setPendingDays(Math.max(0, balance.getPendingDays() - days));
            balance.setUpdatedAt(LocalDateTime.now());
            balanceMapper.updateById(balance);
        }
        logChange(userId, leaveType, year, "CANCEL", days, leaveId, "审批取消退回");
    }

    private void logChange(long userId, String leaveType, int year, String changeType, double days, Long leaveId, String remark) {
        AuthUser operator = authService.currentUser();
        LeaveBalanceLog log = new LeaveBalanceLog();
        log.setUserId(userId);
        log.setLeaveType(leaveType);
        log.setYear(year);
        log.setChangeType(changeType);
        log.setDays(days);
        log.setRelatedLeaveId(leaveId);
        log.setRemark(remark);
        log.setOperatorId(operator.id());
        log.setDeleted(0);
        balanceLogMapper.insert(log);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> teamLeaveCalendar(java.time.LocalDate start, java.time.LocalDate end) {
        // Returns list of team members' leave periods within date range
        // This is used for the team calendar view
        AuthUser user = authService.currentUser();
        // Query org_user_dept to find team members, then check oa_leave for overlapping dates
        return java.util.List.of(); // Placeholder - implement with proper team member query
    }

    /**
     * 预测请假余额耗尽时间
     * 基于过去12个月的使用趋势，预测各类假期何时耗尽
     */
    @Transactional(readOnly = true)
    public Map<String, Object> predictBalanceExhaustion() {
        AuthUser user = authService.currentUser();
        int year = java.time.Year.now().getValue();
        java.time.LocalDate today = java.time.LocalDate.now();

        // 获取当前余额
        List<Map<String, Object>> balances = getMyBalance();

        // 获取过去12个月的请假记录
        java.time.LocalDateTime twelveMonthsAgo = today.minusMonths(12).atStartOfDay();
        List<com.company.oa.entity.oa.OaLeave> leaves = leaveMapper.selectList(
            new LambdaQueryWrapper<com.company.oa.entity.oa.OaLeave>()
                .eq(com.company.oa.entity.oa.OaLeave::getCreatedBy, user.id())
                .eq(com.company.oa.entity.oa.OaLeave::getDeleted, 0)
                .ge(com.company.oa.entity.oa.OaLeave::getCreatedAt, twelveMonthsAgo)
                .in(com.company.oa.entity.oa.OaLeave::getStatus, "APPROVED", "COMPLETED")
        );

        // 按类型统计月均使用量
        Map<String, Double> monthlyUsageByType = new java.util.HashMap<>();
        Map<String, Integer> usageCountByType = new java.util.HashMap<>();

        for (com.company.oa.entity.oa.OaLeave leave : leaves) {
            String type = leave.getLeaveType();
            double days = leave.getDurationDays() != null ? leave.getDurationDays().doubleValue() : 0;
            monthlyUsageByType.merge(type, days, Double::sum);
            usageCountByType.merge(type, 1, Integer::sum);
        }

        // 计算月均使用量（基于过去12个月）
        Map<String, Double> avgMonthlyUsage = new java.util.HashMap<>();
        for (Map.Entry<String, Double> entry : monthlyUsageByType.entrySet()) {
            // 至少有1个月的数据，最多12个月
            int months = Math.max(1, Math.min(12, usageCountByType.getOrDefault(entry.getKey(), 1)));
            avgMonthlyUsage.put(entry.getKey(), entry.getValue() / months);
        }

        // 预测各类假期耗尽时间
        List<Map<String, Object>> predictions = new java.util.ArrayList<>();
        for (Map<String, Object> balance : balances) {
            String typeCode = (String) balance.get("typeCode");
            String typeName = (String) balance.get("typeName");
            double remainingDays = ((Number) balance.get("remainingDays")).doubleValue();

            Map<String, Object> prediction = new java.util.LinkedHashMap<>();
            prediction.put("typeCode", typeCode);
            prediction.put("typeName", typeName);
            prediction.put("remainingDays", remainingDays);

            Double monthlyUsage = avgMonthlyUsage.get(typeCode);
            if (monthlyUsage != null && monthlyUsage > 0) {
                // 预测耗尽月份
                double monthsUntilExhaustion = remainingDays / monthlyUsage;
                java.time.LocalDate exhaustionDate = today.plusMonths((long) Math.ceil(monthsUntilExhaustion));
                prediction.put("avgMonthlyUsage", Math.round(monthlyUsage * 10) / 10.0);
                prediction.put("predictedExhaustionDate", exhaustionDate.toString());
                prediction.put("monthsRemaining", Math.round(monthsUntilExhaustion * 10) / 10.0);

                // 风险等级
                if (monthsUntilExhaustion <= 2) {
                    prediction.put("riskLevel", "HIGH");
                    prediction.put("riskMessage", "预计将在 " + exhaustionDate.getMonthValue() + " 月耗尽，建议合理规划");
                } else if (monthsUntilExhaustion <= 4) {
                    prediction.put("riskLevel", "MEDIUM");
                    prediction.put("riskMessage", "预计可用至 " + exhaustionDate.getMonthValue() + " 月");
                } else {
                    prediction.put("riskLevel", "LOW");
                    prediction.put("riskMessage", "余额充足");
                }
            } else {
                prediction.put("avgMonthlyUsage", 0);
                prediction.put("predictedExhaustionDate", null);
                prediction.put("monthsRemaining", -1); // -1 表示无使用记录
                prediction.put("riskLevel", "LOW");
                prediction.put("riskMessage", "近期无使用记录");
            }
            predictions.add(prediction);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userId", user.id());
        result.put("year", year);
        result.put("predictions", predictions);
        return result;
    }
}
