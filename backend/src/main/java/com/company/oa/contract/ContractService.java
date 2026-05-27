package com.company.oa.contract;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.service.OaSnapshotUtils;
import com.company.oa.common.service.OaUtils;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.contract.ContractInfo;
import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.workflow.WorkflowService;
import com.company.oa.common.service.SequenceService;
import com.company.oa.common.service.OaPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {
    private static final String DRAFT = "DRAFT";
    private static final String APPROVING = "APPROVING";
    private static final String APPROVED = "APPROVED";
    private static final String SIGNED = "SIGNED";
    private static final String TERMINATED = "TERMINATED";

    private final ContractInfoMapper contractMapper;
    private final PaginationHelper paginationHelper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public ContractService(
            ContractInfoMapper contractMapper,
            PaginationHelper paginationHelper,
            UserMapper userMapper,
            AuthService authService,
            WorkflowService workflowService,
            AuditService auditService,
            SequenceService sequenceService,
            ObjectMapper objectMapper
    ) {
        this.contractMapper = contractMapper;
        this.paginationHelper = paginationHelper;
        this.userMapper = userMapper;
        this.authService = authService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Long ownerId) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        LambdaQueryWrapper<ContractInfo> qw = new LambdaQueryWrapper<>();
        if (ownerId != null) {
            if (!user.permissions().contains("*") && !user.id().equals(ownerId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人合同");
            }
            qw.eq(ContractInfo::getCreatedBy, ownerId);
        } else {
            qw.eq(ContractInfo::getCreatedBy, user.id());
        }
        long total = contractMapper.selectCount(qw);
        qw.select(ContractInfo::getId, ContractInfo::getContractNo, ContractInfo::getContractName, ContractInfo::getContractType,
                        ContractInfo::getCounterparty, ContractInfo::getAmount, ContractInfo::getStartDate, ContractInfo::getEndDate,
                        ContractInfo::getSignDate, ContractInfo::getStatus, ContractInfo::getProcessInstanceId,
                        ContractInfo::getWfInstanceId, ContractInfo::getCreatedAt, ContractInfo::getUpdatedAt)
                .orderByDesc(ContractInfo::getId)
                .last("limit " + ps[1] + " offset " + ((ps[0] - 1) * ps[1]));
        List<ContractInfo> contracts = contractMapper.selectList(qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (ContractInfo c : contracts) {
            items.add(toMap(c));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        Map<String, Object> row = loadContract(id);
        OaPermissionUtils.assertViewAllowed(row, authService, "此记录");
        return row;
    }

    @Transactional
    public Map<String, Object> create(ContractDtos.ContractCreateRequest req) {
        AuthUser user = authService.currentUser();
        long newId = sequenceService.nextId("contract_info");
        String contractNo = formatContractNo(newId);
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> snap = OaSnapshotUtils.loadUserDeptSnapshot(user.id(), userMapper);
        ContractInfo entity = new ContractInfo();
        entity.setId(newId);
        entity.setContractNo(contractNo);
        entity.setContractName(req.contractName());
        entity.setContractType(req.contractType());
        entity.setCounterparty(req.counterparty());
        entity.setAmount(req.amount());
        entity.setStartDate(req.startDate());
        entity.setEndDate(req.endDate());
        entity.setStatus(DRAFT);
        entity.setCreatedBy(user.id());
        entity.setCreatedNameSnapshot(user.realName());
        entity.setCreatedDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty() ? null : Long.parseLong(snap.get("deptId")));
        entity.setCreatedDeptNameSnapshot(snap.get("deptName"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        contractMapper.insert(entity);
        return detail(newId);
    }

    @Transactional
    public Map<String, Object> update(long id, ContractDtos.ContractUpdateRequest req) {
        Map<String, Object> row = loadContract(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可编辑");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ContractInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(ContractInfo::getId, id)
                .set(ContractInfo::getContractName, req.contractName())
                .set(ContractInfo::getContractType, req.contractType())
                .set(ContractInfo::getCounterparty, req.counterparty())
                .set(ContractInfo::getAmount, req.amount())
                .set(ContractInfo::getStartDate, req.startDate())
                .set(ContractInfo::getEndDate, req.endDate())
                .set(ContractInfo::getUpdatedAt, now)
                .setSql("version = version + 1");
        contractMapper.update(null, uw);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> submit(long id) {
        Map<String, Object> row = loadContract(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可提交");
        }
        String title = "合同-" + row.get("contractNo");
        Map<String, Object> wf = workflowService.startInstance(new WorkflowDtos.StartInstanceRequest(
                "CONTRACT",
                id,
                title,
                Map.of("contractAmount", row.get("amount"))
        ));
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ContractInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(ContractInfo::getId, id)
                .set(ContractInfo::getStatus, APPROVING)
                .set(ContractInfo::getProcessInstanceId, (String) wf.get("processInstanceId"))
                .set(ContractInfo::getWfInstanceId, OaUtils.toLong(wf.get("wfInstanceId")))
                .set(ContractInfo::getUpdatedAt, now)
                .setSql("version = version + 1");
        contractMapper.update(null, uw);
        Map<String, Object> out = detail(id);
        out.put("currentNodeName", wf.get("currentNodeName"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "CONTRACT_SUBMIT", "CONTRACT", id, AuditService.SUCCESS, null);
        return out;
    }

    @Transactional
    public Map<String, Object> signContract(long id) {
        Map<String, Object> row = loadContract(id);
        assertAdminOnly();
        if (!APPROVED.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已通过可标记签署");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LambdaUpdateWrapper<ContractInfo> uw = new LambdaUpdateWrapper<>();
        uw.eq(ContractInfo::getId, id)
                .set(ContractInfo::getStatus, SIGNED)
                .set(ContractInfo::getSignDate, today)
                .set(ContractInfo::getUpdatedAt, now)
                .setSql("version = version + 1");
        contractMapper.update(null, uw);
        auditService.safeRecordOperation(authService.currentUser().id(),
                "CONTRACT_SIGN", "CONTRACT", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> terminateContract(long id) {
        Map<String, Object> row = loadContract(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        String st = String.valueOf(row.get("status"));
        if (APPROVING.equals(st)) {
            Object wfInst = row.get("wfInstanceId");
            if (wfInst == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "未关联流程实例");
            }
            workflowService.terminateInstance(((Number) wfInst).longValue());
        } else if (APPROVED.equals(st) || SIGNED.equals(st)) {
            LocalDateTime now = LocalDateTime.now();
            LambdaUpdateWrapper<ContractInfo> uw = new LambdaUpdateWrapper<>();
            uw.eq(ContractInfo::getId, id)
                    .set(ContractInfo::getStatus, TERMINATED)
                    .set(ContractInfo::getProcessInstanceId, (Long) null)
                    .set(ContractInfo::getWfInstanceId, (Long) null)
                    .set(ContractInfo::getUpdatedAt, now)
                    .setSql("version = version + 1");
            contractMapper.update(null, uw);
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不可终止");
        }
        auditService.safeRecordOperation(authService.currentUser().id(),
                "CONTRACT_TERMINATE", "CONTRACT", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> renewContract(long id) {
        Map<String, Object> row = loadContract(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        String st = String.valueOf(row.get("status"));
        if (!APPROVED.equals(st) && !SIGNED.equals(st)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已通过或已签署可续签草稿");
        }
        long newId = sequenceService.nextId("contract_info");
        String contractNo = formatContractNo(newId);
        LocalDateTime now = LocalDateTime.now();
        AuthUser user = authService.currentUser();
        Map<String, String> snap = OaSnapshotUtils.loadUserDeptSnapshot(user.id(), userMapper);
        ContractInfo entity = new ContractInfo();
        entity.setId(newId);
        entity.setContractNo(contractNo);
        entity.setContractName(String.valueOf(row.get("contractName")) + "（续签）");
        entity.setContractType(String.valueOf(row.get("contractType")));
        entity.setCounterparty(String.valueOf(row.get("counterparty")));
        entity.setAmount(new BigDecimal(String.valueOf(row.get("amount"))));
        entity.setStartDate(row.get("startDate") instanceof LocalDate ? (LocalDate) row.get("startDate") : null);
        entity.setEndDate(row.get("endDate") instanceof LocalDate ? (LocalDate) row.get("endDate") : null);
        entity.setStatus(DRAFT);
        entity.setCreatedBy(user.id());
        entity.setCreatedNameSnapshot(user.realName());
        entity.setCreatedDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty() ? null : Long.parseLong(snap.get("deptId")));
        entity.setCreatedDeptNameSnapshot(snap.get("deptName"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        contractMapper.insert(entity);
        return detail(newId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> versions(long id) {
        Map<String, Object> row = loadContract(id);
        OaPermissionUtils.assertViewAllowed(row, authService, "此记录");
        return List.of();
    }

    private Map<String, Object> loadContract(long id) {
        ContractInfo entity = contractMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "合同不存在");
        }
        Map<String, Object> map = toMap(entity);
        map.remove("deleted");
        return new LinkedHashMap<>(map);
    }


    private void assertAdminOnly() {
        AuthUser user = authService.currentUser();
        if (user.permissions().contains("*") || user.permissions().contains("contract:manage")) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "仅合同管理员可执行此操作");
    }



    private static String formatContractNo(long id) {
        return "HT" + String.format("%012d", id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }

}
