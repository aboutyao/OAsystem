package com.company.oa.workflow;

import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.oa.mapper.OaSealApplyMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Syncs OA document status from workflow terminal states.
 * Extracted from WorkflowService for reuse across services.
 */
public final class WorkflowDocumentSyncer {
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String CANCELLED = "CANCELLED";

    private WorkflowDocumentSyncer() {
    }

    public static void sync(long wfInstanceId, String wfTerminalStatus, WfProcessInstanceMapper instanceMapper) {
        // Load instance to get business type and ID
        Map<String, Object> inst = instanceMapper.loadInstanceMap(wfInstanceId);
        if (inst == null) return;
        String businessType = String.valueOf(inst.get("businessType"));
        Object bidObj = inst.get("businessId");
        if (bidObj == null) return;
        long businessId = ((Number) bidObj).longValue();

        String docStatus;
        boolean clearFlowKeys;
        switch (wfTerminalStatus) {
            case APPROVED -> { docStatus = APPROVED; clearFlowKeys = false; }
            case REJECTED -> { docStatus = REJECTED; clearFlowKeys = true; }
            case WITHDRAWN -> { docStatus = WITHDRAWN; clearFlowKeys = true; }
            case TERMINATED -> { docStatus = CANCELLED; clearFlowKeys = true; }
            default -> { return; }
        }
        LocalDateTime now = LocalDateTime.now();
        switch (businessType) {
            case "LEAVE" -> {
                var mapper = getLeaveMapper();
                if (clearFlowKeys) mapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else mapper.updateStatusById(businessId, docStatus, now);
            }
            case "EXPENSE", "EXPENSE_HIGH" -> {
                var mapper = getExpenseMapper();
                if (clearFlowKeys) mapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else mapper.updateStatusById(businessId, docStatus, now);
            }
            case "SEAL" -> {
                var mapper = getSealMapper();
                if (clearFlowKeys) mapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else mapper.updateStatusById(businessId, docStatus, now);
            }
            case "PURCHASE" -> {
                var mapper = getPurchaseMapper();
                if (clearFlowKeys) mapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else mapper.updateStatusById(businessId, docStatus, now);
            }
            case "CONTRACT" -> {
                var mapper = getContractMapper();
                if (clearFlowKeys) mapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else mapper.updateStatusById(businessId, docStatus, now);
            }
        }
    }

    // Lazy-initialized mappers — injected via Spring in WorkflowService
    private static OaLeaveMapper leaveMapper;
    private static OaExpenseMapper expenseMapper;
    private static OaSealApplyMapper sealApplyMapper;
    private static OaPurchaseMapper purchaseMapper;
    private static ContractInfoMapper contractMapper;

    public static void init(OaLeaveMapper lm, OaExpenseMapper em, OaSealApplyMapper sm,
                            OaPurchaseMapper pm, ContractInfoMapper cm) {
        leaveMapper = lm;
        expenseMapper = em;
        sealApplyMapper = sm;
        purchaseMapper = pm;
        contractMapper = cm;
    }

    private static OaLeaveMapper getLeaveMapper() { return leaveMapper; }
    private static OaExpenseMapper getExpenseMapper() { return expenseMapper; }
    private static OaSealApplyMapper getSealMapper() { return sealApplyMapper; }
    private static OaPurchaseMapper getPurchaseMapper() { return purchaseMapper; }
    private static ContractInfoMapper getContractMapper() { return contractMapper; }
}
