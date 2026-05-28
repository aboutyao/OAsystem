package com.company.oa.workflow;

import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.oa.OaExpense;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.entity.oa.OaSealApply;
import com.company.oa.entity.org.User;
import com.company.oa.entity.wf.WfProcessInstance;
import com.company.oa.entity.wf.WfTaskRecord;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.oa.mapper.OaSealApplyMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import com.company.oa.workflow.mapper.WfTaskRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowContextService {

    private final WfTaskMapper wfTaskMapper;
    private final WfProcessInstanceMapper instanceMapper;
    private final WfTaskRecordMapper taskRecordMapper;
    private final UserMapper userMapper;
    private final OaLeaveMapper leaveMapper;
    private final OaExpenseMapper expenseMapper;
    private final OaPurchaseMapper purchaseMapper;
    private final OaSealApplyMapper sealApplyMapper;

    public WorkflowContextService(
            WfTaskMapper wfTaskMapper,
            WfProcessInstanceMapper instanceMapper,
            WfTaskRecordMapper taskRecordMapper,
            UserMapper userMapper,
            OaLeaveMapper leaveMapper,
            OaExpenseMapper expenseMapper,
            OaPurchaseMapper purchaseMapper,
            OaSealApplyMapper sealApplyMapper
    ) {
        this.wfTaskMapper = wfTaskMapper;
        this.instanceMapper = instanceMapper;
        this.taskRecordMapper = taskRecordMapper;
        this.userMapper = userMapper;
        this.leaveMapper = leaveMapper;
        this.expenseMapper = expenseMapper;
        this.purchaseMapper = purchaseMapper;
        this.sealApplyMapper = sealApplyMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getApprovalContext(long wfTaskId) {
        // 1. Load the wf_task to get wfInstanceId and the current task info
        Map<String, Object> taskRow = loadWfTask(wfTaskId);
        long wfInstanceId = ((Number) taskRow.get("wfInstanceId")).longValue();

        // 2. Load the process instance to get the requester (starter) and business type
        Map<String, Object> instRow = loadInstance(wfInstanceId);
        long starterId = ((Number) instRow.get("starterId")).longValue();
        String businessType = String.valueOf(instRow.get("businessType"));
        Object businessIdObj = instRow.get("businessId");
        Long businessId = businessIdObj == null ? null : ((Number) businessIdObj).longValue();

        // 3. Build requester info
        Map<String, Object> requesterInfo = buildRequesterInfo(starterId);

        // 4. Query recent 5 approval records for this user across all workflows
        List<Map<String, Object>> requesterHistory = buildRequesterHistory(starterId);

        // 5. Calculate comparison averages for the same business type
        Map<String, Object> peerComparison = buildPeerComparison(businessType, starterId, businessId);

        // 6. Build risk flags
        List<String> riskFlags = buildRiskFlags(starterId, businessType, businessId, instRow);

        // Assemble final context
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("wfTaskId", wfTaskId);
        context.put("wfInstanceId", wfInstanceId);
        context.put("businessType", businessType);
        context.put("businessId", businessId);
        context.put("title", instRow.get("title"));
        context.put("requester", requesterInfo);
        context.put("requesterHistory", requesterHistory);
        context.put("peerComparison", peerComparison);
        context.put("riskFlags", riskFlags);
        return context;
    }

    // ─── Private helpers ───────────────────────────────────────────────

    private Map<String, Object> loadWfTask(long wfTaskId) {
        Map<String, Object> row = wfTaskMapper.loadWfTask(wfTaskId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return new LinkedHashMap<>(row);
    }

    private Map<String, Object> loadInstance(long wfInstanceId) {
        Map<String, Object> row = instanceMapper.loadInstance(wfInstanceId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程实例不存在");
        }
        return new LinkedHashMap<>(row);
    }

    /**
     * Build requester profile: name, department, position, entry date.
     * Uses selectUserDetailById which joins org_dept, org_position, org_rank.
     */
    private Map<String, Object> buildRequesterInfo(long userId) {
        User user = userMapper.selectById(userId);
        Map<String, Object> detail = userMapper.selectUserDetailById(userId);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", userId);
        if (detail != null && !detail.isEmpty()) {
            info.put("realName", detail.get("realName"));
            info.put("employeeNo", detail.get("employeeNo"));
            info.put("departmentName", detail.get("mainDeptName"));
            info.put("positionName", detail.get("positionName"));
            info.put("rankName", detail.get("rankName"));
            info.put("entryDate", detail.get("entryDate"));
            info.put("mobile", detail.get("mobile"));
            info.put("email", detail.get("email"));
            info.put("managerName", detail.get("managerName"));
        } else if (user != null) {
            info.put("realName", user.getRealName());
            info.put("employeeNo", user.getEmployeeNo());
            info.put("entryDate", user.getEntryDate());
            info.put("mobile", user.getMobile());
            info.put("email", user.getEmail());
        }
        // Calculate tenure in days
        if (user != null && user.getEntryDate() != null) {
            long tenureDays = ChronoUnit.DAYS.between(user.getEntryDate(), LocalDate.now());
            info.put("tenureDays", tenureDays);
        }
        return info;
    }

    /**
     * Query recent 5 approval records (as operator) for the requester,
     * covering all workflow instances where this user acted.
     */
    private List<Map<String, Object>> buildRequesterHistory(long userId) {
        List<WfTaskRecord> records = taskRecordMapper.selectList(
                new LambdaQueryWrapper<WfTaskRecord>()
                        .eq(WfTaskRecord::getOperatorId, userId)
                        .in(WfTaskRecord::getAction, "APPROVE", "REJECT")
                        .orderByDesc(WfTaskRecord::getOperatedAt)
                        .last("limit 5")
        );
        List<Map<String, Object>> history = new ArrayList<>();
        for (WfTaskRecord record : records) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("action", record.getAction());
            entry.put("nodeName", record.getNodeName());
            entry.put("comment", record.getComment());
            entry.put("operatedAt", record.getOperatedAt() != null ? record.getOperatedAt().toString() : null);
            history.add(entry);
        }
        return history;
    }

    /**
     * Calculate average amount/days for the same business type across all approved records,
     * excluding the current request. Also includes department-level averages.
     */
    private Map<String, Object> buildPeerComparison(String businessType, long starterId, Long currentBusinessId) {
        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("businessType", businessType);

        switch (businessType) {
            case "LEAVE" -> {
                // Current request value
                OaLeave currentLeave = currentBusinessId != null ? leaveMapper.selectById(currentBusinessId) : null;
                BigDecimal currentDays = currentLeave != null ? currentLeave.getDurationDays() : null;
                comparison.put("currentValue", currentDays);
                comparison.put("unit", "days");

                // All approved leaves (excluding current)
                LambdaQueryWrapper<OaLeave> leaveQw = new LambdaQueryWrapper<OaLeave>()
                        .eq(OaLeave::getStatus, "APPROVED")
                        .isNotNull(OaLeave::getDurationDays);
                if (currentBusinessId != null) {
                    leaveQw.ne(OaLeave::getId, currentBusinessId);
                }
                List<OaLeave> allApprovedLeaves = leaveMapper.selectList(leaveQw);

                // Overall average
                if (!allApprovedLeaves.isEmpty()) {
                    BigDecimal totalDays = allApprovedLeaves.stream()
                            .map(OaLeave::getDurationDays)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avgDays = totalDays.divide(BigDecimal.valueOf(allApprovedLeaves.size()), 2, RoundingMode.HALF_UP);
                    comparison.put("overallAverage", avgDays);
                    comparison.put("overallCount", allApprovedLeaves.size());
                }

                // Department average
                User user = userMapper.selectById(starterId);
                if (user != null && user.getMainDeptId() != null) {
                    List<OaLeave> deptLeaves = allApprovedLeaves.stream()
                            .filter(l -> user.getMainDeptId().equals(l.getCreatedDeptId()))
                            .toList();
                    if (!deptLeaves.isEmpty()) {
                        BigDecimal deptTotal = deptLeaves.stream()
                                .map(OaLeave::getDurationDays)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal deptAvg = deptTotal.divide(BigDecimal.valueOf(deptLeaves.size()), 2, RoundingMode.HALF_UP);
                        comparison.put("departmentAverage", deptAvg);
                        comparison.put("departmentCount", deptLeaves.size());
                    }
                }
            }
            case "EXPENSE", "EXPENSE_HIGH" -> {
                OaExpense currentExpense = currentBusinessId != null ? expenseMapper.selectById(currentBusinessId) : null;
                BigDecimal currentAmount = currentExpense != null ? currentExpense.getTotalAmount() : null;
                comparison.put("currentValue", currentAmount);
                comparison.put("unit", "amount");

                LambdaQueryWrapper<OaExpense> expQw = new LambdaQueryWrapper<OaExpense>()
                        .eq(OaExpense::getStatus, "APPROVED")
                        .isNotNull(OaExpense::getTotalAmount);
                if (currentBusinessId != null) {
                    expQw.ne(OaExpense::getId, currentBusinessId);
                }
                List<OaExpense> allApproved = expenseMapper.selectList(expQw);

                if (!allApproved.isEmpty()) {
                    BigDecimal total = allApproved.stream()
                            .map(OaExpense::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = total.divide(BigDecimal.valueOf(allApproved.size()), 2, RoundingMode.HALF_UP);
                    comparison.put("overallAverage", avg);
                    comparison.put("overallCount", allApproved.size());
                }

                User user = userMapper.selectById(starterId);
                if (user != null && user.getMainDeptId() != null) {
                    List<OaExpense> deptItems = allApproved.stream()
                            .filter(e -> user.getMainDeptId().equals(e.getCreatedDeptId()))
                            .toList();
                    if (!deptItems.isEmpty()) {
                        BigDecimal deptTotal = deptItems.stream()
                                .map(OaExpense::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal deptAvg = deptTotal.divide(BigDecimal.valueOf(deptItems.size()), 2, RoundingMode.HALF_UP);
                        comparison.put("departmentAverage", deptAvg);
                        comparison.put("departmentCount", deptItems.size());
                    }
                }
            }
            case "PURCHASE" -> {
                OaPurchase current = currentBusinessId != null ? purchaseMapper.selectById(currentBusinessId) : null;
                BigDecimal currentAmount = current != null ? current.getTotalAmount() : null;
                comparison.put("currentValue", currentAmount);
                comparison.put("unit", "amount");

                LambdaQueryWrapper<OaPurchase> qQw = new LambdaQueryWrapper<OaPurchase>()
                        .eq(OaPurchase::getStatus, "APPROVED")
                        .isNotNull(OaPurchase::getTotalAmount);
                if (currentBusinessId != null) {
                    qQw.ne(OaPurchase::getId, currentBusinessId);
                }
                List<OaPurchase> allApproved = purchaseMapper.selectList(qQw);

                if (!allApproved.isEmpty()) {
                    BigDecimal total = allApproved.stream()
                            .map(OaPurchase::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = total.divide(BigDecimal.valueOf(allApproved.size()), 2, RoundingMode.HALF_UP);
                    comparison.put("overallAverage", avg);
                    comparison.put("overallCount", allApproved.size());
                }

                User user = userMapper.selectById(starterId);
                if (user != null && user.getMainDeptId() != null) {
                    List<OaPurchase> deptItems = allApproved.stream()
                            .filter(p -> user.getMainDeptId().equals(p.getCreatedDeptId()))
                            .toList();
                    if (!deptItems.isEmpty()) {
                        BigDecimal deptTotal = deptItems.stream()
                                .map(OaPurchase::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal deptAvg = deptTotal.divide(BigDecimal.valueOf(deptItems.size()), 2, RoundingMode.HALF_UP);
                        comparison.put("departmentAverage", deptAvg);
                        comparison.put("departmentCount", deptItems.size());
                    }
                }
            }
            case "SEAL" -> {
                // Seal applications don't have a monetary value; compare by recent submission frequency
                comparison.put("currentValue", null);
                comparison.put("unit", "count");
                LambdaQueryWrapper<OaSealApply> sealQw = new LambdaQueryWrapper<OaSealApply>()
                        .eq(OaSealApply::getStatus, "APPROVED");
                long totalSealCount = sealApplyMapper.selectCount(sealQw);
                comparison.put("overallCount", totalSealCount);
            }
        }
        return comparison;
    }

    /**
     * Build a list of risk indicators for the current request.
     */
    private List<String> buildRiskFlags(long starterId, String businessType, Long businessId, Map<String, Object> instRow) {
        List<String> flags = new ArrayList<>();
        User user = userMapper.selectById(starterId);
        if (user == null) {
            return flags;
        }

        // ── Risk: 首次提交此类申请 ──
        boolean isFirst = isFirstSubmission(starterId, businessType);
        if (isFirst) {
            flags.add("首次提交" + businessTypeToLabel(businessType) + "申请");
        }

        // ── Risk: 入职不足30天 ──
        if (user.getEntryDate() != null) {
            long tenureDays = ChronoUnit.DAYS.between(user.getEntryDate(), LocalDate.now());
            if (tenureDays < 30) {
                flags.add("入职不足30天（已入职" + tenureDays + "天）");
            }
        }

        // ── Risk: 近30天请假天数 ──
        if ("LEAVE".equals(businessType) && businessId != null) {
            OaLeave currentLeave = leaveMapper.selectById(businessId);
            if (currentLeave != null) {
                BigDecimal recentLeaveDays = calculateRecentLeaveDays(starterId, businessId);
                if (recentLeaveDays.compareTo(BigDecimal.ZERO) > 0) {
                    flags.add("近30天已请假" + recentLeaveDays.stripTrailingZeros().toPlainString() + "天");
                }
            }
        }

        // ── Risk: 金额高于部门平均 ──
        if (businessId != null && user.getMainDeptId() != null) {
            String amountFlag = checkAmountAboveDeptAvg(businessType, businessId, user.getMainDeptId());
            if (amountFlag != null) {
                flags.add(amountFlag);
            }
        }

        // ── Risk: 金额高于部门最高 ──
        if (businessId != null && user.getMainDeptId() != null) {
            String maxFlag = checkAmountAboveDeptMax(businessType, businessId, user.getMainDeptId());
            if (maxFlag != null) {
                flags.add(maxFlag);
            }
        }

        // ── Risk: 近期被驳回过 ──
        long recentRejectionCount = countRecentRejections(starterId);
        if (recentRejectionCount > 0) {
            flags.add("近30天内有" + recentRejectionCount + "次审批被驳回");
        }

        // ── Risk: 同一业务重复提交 ──
        if (isDuplicateSubmission(starterId, businessType, businessId)) {
            flags.add("同一业务类型短期内重复提交");
        }

        return flags;
    }

    /**
     * Check whether this is the user's first submission for this business type.
     */
    private boolean isFirstSubmission(long userId, String businessType) {
        Long count = instanceMapper.selectCount(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, userId)
                        .eq(WfProcessInstance::getBusinessType, businessType)
        );
        return count == null || count == 0;
    }

    /**
     * Calculate total leave days in the past 30 days (approved or approving), excluding the current request.
     */
    private BigDecimal calculateRecentLeaveDays(long userId, Long currentLeaveId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<OaLeave> qw = new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getCreatedBy, userId)
                .in(OaLeave::getStatus, "APPROVED", "APPROVING")
                .ge(OaLeave::getCreatedAt, thirtyDaysAgo);
        if (currentLeaveId != null) {
            qw.ne(OaLeave::getId, currentLeaveId);
        }
        List<OaLeave> recentLeaves = leaveMapper.selectList(qw);
        return recentLeaves.stream()
                .map(l -> l.getDurationDays() != null ? l.getDurationDays() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if the current request amount/days exceeds the department average.
     */
    private String checkAmountAboveDeptAvg(String businessType, long businessId, Long deptId) {
        switch (businessType) {
            case "LEAVE" -> {
                OaLeave current = leaveMapper.selectById(businessId);
                if (current == null || current.getDurationDays() == null) return null;
                List<OaLeave> deptLeaves = leaveMapper.selectList(
                        new LambdaQueryWrapper<OaLeave>()
                                .eq(OaLeave::getCreatedDeptId, deptId)
                                .eq(OaLeave::getStatus, "APPROVED")
                                .ne(OaLeave::getId, businessId)
                );
                if (deptLeaves.isEmpty()) return null;
                BigDecimal total = deptLeaves.stream()
                        .map(l -> l.getDurationDays() != null ? l.getDurationDays() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = total.divide(BigDecimal.valueOf(deptLeaves.size()), 2, RoundingMode.HALF_UP);
                if (current.getDurationDays().compareTo(avg) > 0) {
                    return "请假天数(" + current.getDurationDays().stripTrailingZeros().toPlainString()
                            + ")高于部门平均(" + avg.stripTrailingZeros().toPlainString() + ")";
                }
            }
            case "EXPENSE", "EXPENSE_HIGH" -> {
                OaExpense current = expenseMapper.selectById(businessId);
                if (current == null || current.getTotalAmount() == null) return null;
                List<OaExpense> deptItems = expenseMapper.selectList(
                        new LambdaQueryWrapper<OaExpense>()
                                .eq(OaExpense::getCreatedDeptId, deptId)
                                .eq(OaExpense::getStatus, "APPROVED")
                                .ne(OaExpense::getId, businessId)
                );
                if (deptItems.isEmpty()) return null;
                BigDecimal total = deptItems.stream()
                        .map(e -> e.getTotalAmount() != null ? e.getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = total.divide(BigDecimal.valueOf(deptItems.size()), 2, RoundingMode.HALF_UP);
                if (current.getTotalAmount().compareTo(avg) > 0) {
                    return "金额(" + current.getTotalAmount().stripTrailingZeros().toPlainString()
                            + ")高于部门平均(" + avg.stripTrailingZeros().toPlainString() + ")";
                }
            }
            case "PURCHASE" -> {
                OaPurchase current = purchaseMapper.selectById(businessId);
                if (current == null || current.getTotalAmount() == null) return null;
                List<OaPurchase> deptItems = purchaseMapper.selectList(
                        new LambdaQueryWrapper<OaPurchase>()
                                .eq(OaPurchase::getCreatedDeptId, deptId)
                                .eq(OaPurchase::getStatus, "APPROVED")
                                .ne(OaPurchase::getId, businessId)
                );
                if (deptItems.isEmpty()) return null;
                BigDecimal total = deptItems.stream()
                        .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = total.divide(BigDecimal.valueOf(deptItems.size()), 2, RoundingMode.HALF_UP);
                if (current.getTotalAmount().compareTo(avg) > 0) {
                    return "采购金额(" + current.getTotalAmount().stripTrailingZeros().toPlainString()
                            + ")高于部门平均(" + avg.stripTrailingZeros().toPlainString() + ")";
                }
            }
        }
        return null;
    }

    /**
     * Check if the current request amount/days exceeds the department maximum (outlier detection).
     */
    private String checkAmountAboveDeptMax(String businessType, long businessId, Long deptId) {
        switch (businessType) {
            case "LEAVE" -> {
                OaLeave current = leaveMapper.selectById(businessId);
                if (current == null || current.getDurationDays() == null) return null;
                List<OaLeave> deptLeaves = leaveMapper.selectList(
                        new LambdaQueryWrapper<OaLeave>()
                                .eq(OaLeave::getCreatedDeptId, deptId)
                                .eq(OaLeave::getStatus, "APPROVED")
                                .ne(OaLeave::getId, businessId)
                );
                if (deptLeaves.isEmpty()) return null;
                BigDecimal max = deptLeaves.stream()
                        .map(l -> l.getDurationDays() != null ? l.getDurationDays() : BigDecimal.ZERO)
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                if (current.getDurationDays().compareTo(max) > 0) {
                    return "请假天数(" + current.getDurationDays().stripTrailingZeros().toPlainString()
                            + ")超过部门历史最高(" + max.stripTrailingZeros().toPlainString() + ")";
                }
            }
            case "EXPENSE", "EXPENSE_HIGH" -> {
                OaExpense current = expenseMapper.selectById(businessId);
                if (current == null || current.getTotalAmount() == null) return null;
                List<OaExpense> deptItems = expenseMapper.selectList(
                        new LambdaQueryWrapper<OaExpense>()
                                .eq(OaExpense::getCreatedDeptId, deptId)
                                .eq(OaExpense::getStatus, "APPROVED")
                                .ne(OaExpense::getId, businessId)
                );
                if (deptItems.isEmpty()) return null;
                BigDecimal max = deptItems.stream()
                        .map(e -> e.getTotalAmount() != null ? e.getTotalAmount() : BigDecimal.ZERO)
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                if (current.getTotalAmount().compareTo(max) > 0) {
                    return "金额(" + current.getTotalAmount().stripTrailingZeros().toPlainString()
                            + ")超过部门历史最高(" + max.stripTrailingZeros().toPlainString() + ")";
                }
            }
            case "PURCHASE" -> {
                OaPurchase current = purchaseMapper.selectById(businessId);
                if (current == null || current.getTotalAmount() == null) return null;
                List<OaPurchase> deptItems = purchaseMapper.selectList(
                        new LambdaQueryWrapper<OaPurchase>()
                                .eq(OaPurchase::getCreatedDeptId, deptId)
                                .eq(OaPurchase::getStatus, "APPROVED")
                                .ne(OaPurchase::getId, businessId)
                );
                if (deptItems.isEmpty()) return null;
                BigDecimal max = deptItems.stream()
                        .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                if (current.getTotalAmount().compareTo(max) > 0) {
                    return "采购金额(" + current.getTotalAmount().stripTrailingZeros().toPlainString()
                            + ")超过部门历史最高(" + max.stripTrailingZeros().toPlainString() + ")";
                }
            }
        }
        return null;
    }

    /**
     * Count the user's rejections in the past 30 days.
     */
    private long countRecentRejections(long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<WfTaskRecord> rejections = taskRecordMapper.selectList(
                new LambdaQueryWrapper<WfTaskRecord>()
                        .eq(WfTaskRecord::getOperatorId, userId)
                        .eq(WfTaskRecord::getAction, "REJECT")
                        .ge(WfTaskRecord::getOperatedAt, thirtyDaysAgo)
        );
        return rejections.size();
    }

    /**
     * Check if the user has recently submitted another request of the same business type (within 7 days).
     */
    private boolean isDuplicateSubmission(long userId, String businessType, Long currentBusinessId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long count = instanceMapper.selectCount(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, userId)
                        .eq(WfProcessInstance::getBusinessType, businessType)
                        .ge(WfProcessInstance::getStartedAt, sevenDaysAgo)
        );
        // count > 1 means there's at least one other recent submission besides the current one
        return count != null && count > 1;
    }

    private String businessTypeToLabel(String businessType) {
        return switch (businessType) {
            case "LEAVE" -> "请假";
            case "EXPENSE", "EXPENSE_HIGH" -> "报销";
            case "PURCHASE" -> "采购";
            case "SEAL" -> "用章";
            case "CONTRACT" -> "合同";
            default -> businessType;
        };
    }
}
