package com.company.oa.workflow;

import com.company.oa.entity.oa.*;
import com.company.oa.entity.wf.WfProcessInstance;
import com.company.oa.oa.mapper.*;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能摘要生成服务
 * 自动为审批单生成摘要
 */
@Service
public class WorkflowSummaryService {
    private final OaLeaveMapper leaveMapper;
    private final OaExpenseMapper expenseMapper;
    private final OaPurchaseMapper purchaseMapper;
    private final OaSealApplyMapper sealApplyMapper;
    private final WfProcessInstanceMapper instanceMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    public WorkflowSummaryService(OaLeaveMapper leaveMapper, OaExpenseMapper expenseMapper,
                                   OaPurchaseMapper purchaseMapper, OaSealApplyMapper sealApplyMapper,
                                   WfProcessInstanceMapper instanceMapper) {
        this.leaveMapper = leaveMapper;
        this.expenseMapper = expenseMapper;
        this.purchaseMapper = purchaseMapper;
        this.sealApplyMapper = sealApplyMapper;
        this.instanceMapper = instanceMapper;
    }

    /**
     * 生成智能摘要
     */
    public String generateSummary(long wfInstanceId) {
        WfProcessInstance instance = instanceMapper.selectById(wfInstanceId);
        if (instance == null) {
            return "流程实例不存在";
        }

        String businessType = instance.getBusinessType();
        Long businessId = instance.getBusinessId();

        if (businessType == null || businessId == null) {
            return instance.getTitle();
        }

        return switch (businessType) {
            case "LEAVE" -> generateLeaveSummary(businessId);
            case "EXPENSE" -> generateExpenseSummary(businessId);
            case "PURCHASE" -> generatePurchaseSummary(businessId);
            case "SEAL" -> generateSealSummary(businessId);
            default -> instance.getTitle();
        };
    }

    private String generateLeaveSummary(Long leaveId) {
        OaLeave leave = leaveMapper.selectById(leaveId);
        if (leave == null) return "请假单不存在";

        StringBuilder sb = new StringBuilder();
        sb.append(leave.getLeaveType());
        sb.append(" | ");
        if (leave.getStartAt() != null && leave.getEndAt() != null) {
            sb.append(leave.getStartAt().format(DATE_FORMAT));
            sb.append(" ~ ");
            sb.append(leave.getEndAt().format(DATE_FORMAT));
        }
        if (leave.getDurationDays() != null) {
            sb.append(" | ").append(leave.getDurationDays()).append("天");
        }
        if (leave.getReason() != null && !leave.getReason().isEmpty()) {
            sb.append(" | ").append(truncate(leave.getReason(), 30));
        }
        return sb.toString();
    }

    private String generateExpenseSummary(Long expenseId) {
        OaExpense expense = expenseMapper.selectById(expenseId);
        if (expense == null) return "报销单不存在";

        StringBuilder sb = new StringBuilder();
        sb.append(expense.getReason() != null ? expense.getReason() : "报销申请");
        sb.append(" | ");
        if (expense.getTotalAmount() != null) {
            sb.append("¥").append(expense.getTotalAmount());
        }
        if (expense.getExpenseType() != null) {
            sb.append(" | ").append(expense.getExpenseType());
        }
        return sb.toString();
    }

    private String generatePurchaseSummary(Long purchaseId) {
        OaPurchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) return "采购单不存在";

        StringBuilder sb = new StringBuilder();
        sb.append(purchase.getPurchaseType() != null ? purchase.getPurchaseType() : "采购申请");
        sb.append(" | ");
        if (purchase.getTotalAmount() != null) {
            sb.append("¥").append(purchase.getTotalAmount());
        }
        if (purchase.getSupplierName() != null) {
            sb.append(" | 供应商: ").append(purchase.getSupplierName());
        }
        return sb.toString();
    }

    private String generateSealSummary(Long sealId) {
        OaSealApply seal = sealApplyMapper.selectById(sealId);
        if (seal == null) return "用章申请不存在";

        StringBuilder sb = new StringBuilder();
        sb.append(seal.getSealType() != null ? seal.getSealType() : "用章申请");
        sb.append(" | ");
        if (seal.getFileTitle() != null) {
            sb.append(seal.getFileTitle());
        }
        if (seal.getUseReason() != null) {
            sb.append(" | ").append(truncate(seal.getUseReason(), 20));
        }
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
