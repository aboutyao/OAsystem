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
    private final AuthService authService;
    private final PaginationHelper paginationHelper;

    public LeaveBalanceService(LeaveTypeMapper leaveTypeMapper, LeaveBalanceMapper balanceMapper,
                               LeaveBalanceLogMapper balanceLogMapper, AuthService authService,
                               PaginationHelper paginationHelper) {
        this.leaveTypeMapper = leaveTypeMapper;
        this.balanceMapper = balanceMapper;
        this.balanceLogMapper = balanceLogMapper;
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
}
