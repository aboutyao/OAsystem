package com.company.oa.oa.purchase;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.entity.oa.OaPurchaseItem;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.oa.mapper.OaPurchaseItemMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.workflow.WorkflowService;
import com.company.oa.common.service.SequenceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseService {
    private static final String DRAFT = "DRAFT";
    private static final String APPROVING = "APPROVING";
    private static final String CANCELLED = "CANCELLED";
    private static final String APPROVED = "APPROVED";
    private static final String NOT_ARRIVED = "NOT_ARRIVED";
    private static final String ARRIVED = "ARRIVED";
    private static final String PENDING = "PENDING";
    private static final String PASSED = "PASSED";

    private final OaPurchaseMapper purchaseMapper;
    private final OaPurchaseItemMapper purchaseItemMapper;
    private final SysConfigMapper sysConfigMapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public PurchaseService(
            OaPurchaseMapper purchaseMapper,
            OaPurchaseItemMapper purchaseItemMapper,
            SysConfigMapper sysConfigMapper,
            UserMapper userMapper,
            AuthService authService,
            WorkflowService workflowService,
            AuditService auditService,
            SequenceService sequenceService,
            ObjectMapper objectMapper
    ) {
        this.purchaseMapper = purchaseMapper;
        this.purchaseItemMapper = purchaseItemMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.userMapper = userMapper;
        this.authService = authService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Long applicantId, String status) {
        AuthUser user = authService.currentUser();
        long[] ps = clampPage(page, size);
        LambdaQueryWrapper<OaPurchase> qw = new LambdaQueryWrapper<>();
        if (applicantId != null) {
            if (!user.permissions().contains("*") && !user.id().equals(applicantId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人采购申请");
            }
            qw.eq(OaPurchase::getCreatedBy, applicantId);
        } else {
            qw.eq(OaPurchase::getCreatedBy, user.id());
        }
        if (status != null && !status.isBlank()) {
            qw.eq(OaPurchase::getStatus, status);
        }
        long total = purchaseMapper.selectCount(qw);
        qw.select(OaPurchase::getId, OaPurchase::getPurchaseNo, OaPurchase::getPurchaseType, OaPurchase::getTotalAmount,
                        OaPurchase::getArrivalStatus, OaPurchase::getAcceptanceStatus, OaPurchase::getStatus,
                        OaPurchase::getProcessInstanceId, OaPurchase::getWfInstanceId,
                        OaPurchase::getCreatedAt, OaPurchase::getUpdatedAt)
                .orderByDesc(OaPurchase::getId)
                .last("limit " + ps[1] + " offset " + ((ps[0] - 1) * ps[1]));
        List<OaPurchase> purchases = purchaseMapper.selectList(qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OaPurchase p : purchases) {
            items.add(toMap(p));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        Map<String, Object> row = loadPurchaseHeader(id);
        assertViewAllowed(row);
        row.put("items", loadPurchaseItems(id));
        return row;
    }

    @Transactional
    public Map<String, Object> create(PurchaseDtos.PurchaseCreateRequest req) {
        BigDecimal sum = sumItems(req.items());
        if (req.totalAmount().compareTo(sum) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请金额与明细合计不一致");
        }
        AuthUser user = authService.currentUser();
        long newId = sequenceService.nextId("oa_purchase");
        String purchaseNo = formatPurchaseNo(newId);
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> snap = loadUserDeptSnapshot(user.id());
        OaPurchase entity = new OaPurchase();
        entity.setId(newId);
        entity.setPurchaseNo(purchaseNo);
        entity.setPurchaseType(req.purchaseType());
        entity.setSupplierName(req.supplierName());
        entity.setBudgetSubject(req.budgetSubject());
        entity.setTotalAmount(req.totalAmount());
        entity.setArrivalStatus(NOT_ARRIVED);
        entity.setAcceptanceStatus(PENDING);
        entity.setReason(req.reason());
        entity.setStatus(DRAFT);
        entity.setCreatedBy(user.id());
        entity.setCreatedNameSnapshot(user.realName());
        entity.setCreatedDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty() ? null : Long.parseLong(snap.get("deptId")));
        entity.setCreatedDeptNameSnapshot(snap.get("deptName"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        purchaseMapper.insert(entity);
        replaceItems(newId, req.items());
        return detail(newId);
    }

    @Transactional
    public Map<String, Object> update(long id, PurchaseDtos.PurchaseUpdateRequest req) {
        Map<String, Object> row = loadPurchaseHeader(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可编辑");
        }
        BigDecimal sum = sumItems(req.items());
        if (req.totalAmount().compareTo(sum) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请金额与明细合计不一致");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaPurchase> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaPurchase::getId, id)
                .set(OaPurchase::getPurchaseType, req.purchaseType())
                .set(OaPurchase::getSupplierName, req.supplierName())
                .set(OaPurchase::getBudgetSubject, req.budgetSubject())
                .set(OaPurchase::getTotalAmount, req.totalAmount())
                .set(OaPurchase::getReason, req.reason())
                .set(OaPurchase::getUpdatedAt, now)
                .setSql("version = version + 1");
        purchaseMapper.update(null, uw);
        replaceItems(id, req.items());
        return detail(id);
    }

    @Transactional
    public Map<String, Object> submit(long id) {
        Map<String, Object> row = loadPurchaseHeader(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可提交");
        }
        String purchaseNo = String.valueOf(row.get("purchaseNo"));
        String title = "采购申请-" + purchaseNo;
        Map<String, Object> wf = workflowService.startInstance(new WorkflowDtos.StartInstanceRequest(
                "PURCHASE",
                id,
                title,
                Map.of("totalAmount", row.get("totalAmount"))
        ));
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaPurchase> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaPurchase::getId, id)
                .set(OaPurchase::getStatus, APPROVING)
                .set(OaPurchase::getProcessInstanceId, (String) wf.get("processInstanceId"))
                .set(OaPurchase::getWfInstanceId, toLong(wf.get("wfInstanceId")))
                .set(OaPurchase::getUpdatedAt, now)
                .setSql("version = version + 1");
        purchaseMapper.update(null, uw);
        Map<String, Object> out = detail(id);
        out.put("currentNodeName", wf.get("currentNodeName"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "PURCHASE_SUBMIT", "PURCHASE", id, AuditService.SUCCESS, null);
        return out;
    }

    @Transactional
    public Map<String, Object> withdrawPurchase(long id) {
        Map<String, Object> row = loadPurchaseHeader(id);
        assertOwner(row);
        if (!APPROVING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审批中可撤回");
        }
        Object wfInst = row.get("wfInstanceId");
        if (wfInst == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未关联流程实例");
        }
        workflowService.withdrawInstance(((Number) wfInst).longValue());
        auditService.safeRecordOperation(authService.currentUser().id(),
                "PURCHASE_WITHDRAW", "PURCHASE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancelPurchase(long id) {
        Map<String, Object> row = loadPurchaseHeader(id);
        assertOwner(row);
        String st = String.valueOf(row.get("status"));
        if (!DRAFT.equals(st) && !APPROVING.equals(st)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不可作废");
        }
        if (APPROVING.equals(st)) {
            Object wfInst = row.get("wfInstanceId");
            if (wfInst != null) {
                workflowService.terminateInstance(((Number) wfInst).longValue());
            }
        } else {
            LocalDateTime now = LocalDateTime.now();
            LambdaUpdateWrapper<OaPurchase> uw = new LambdaUpdateWrapper<>();
            uw.eq(OaPurchase::getId, id)
                    .set(OaPurchase::getStatus, CANCELLED)
                    .set(OaPurchase::getUpdatedAt, now)
                    .setSql("version = version + 1");
            purchaseMapper.update(null, uw);
        }
        auditService.safeRecordOperation(authService.currentUser().id(),
                "PURCHASE_CANCEL", "PURCHASE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> confirmArrival(long id) {
        Map<String, Object> row = loadPurchaseHeader(id);
        assertAdminOnly();
        if (!APPROVED.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已通过可确认到货");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaPurchase> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaPurchase::getId, id)
                .set(OaPurchase::getArrivalStatus, ARRIVED)
                .set(OaPurchase::getUpdatedAt, now)
                .setSql("version = version + 1");
        purchaseMapper.update(null, uw);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> acceptPurchase(long id) {
        Map<String, Object> row = loadPurchaseHeader(id);
        assertAdminOnly();
        if (!APPROVED.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已通过可验收");
        }
        if (!ARRIVED.equals(String.valueOf(row.get("arrivalStatus")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先确认到货");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaPurchase> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaPurchase::getId, id)
                .set(OaPurchase::getAcceptanceStatus, PASSED)
                .set(OaPurchase::getUpdatedAt, now)
                .setSql("version = version + 1");
        purchaseMapper.update(null, uw);
        return detail(id);
    }

    private Map<String, Object> loadPurchaseHeader(long id) {
        OaPurchase entity = purchaseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购申请不存在");
        }
        Map<String, Object> map = toMap(entity);
        map.remove("deleted");
        return new LinkedHashMap<>(map);
    }

    private List<Map<String, Object>> loadPurchaseItems(long purchaseId) {
        LambdaQueryWrapper<OaPurchaseItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OaPurchaseItem::getPurchaseId, purchaseId)
                .orderByAsc(OaPurchaseItem::getSortOrder)
                .orderByAsc(OaPurchaseItem::getId);
        List<OaPurchaseItem> items = purchaseItemMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (OaPurchaseItem item : items) {
            result.add(toMap(item));
        }
        return result;
    }

    private void replaceItems(long purchaseId, List<PurchaseDtos.PurchaseItemRequest> items) {
        LambdaQueryWrapper<OaPurchaseItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OaPurchaseItem::getPurchaseId, purchaseId);
        purchaseItemMapper.delete(qw);
        for (PurchaseDtos.PurchaseItemRequest it : items) {
            long iid = sequenceService.nextId("oa_purchase_item");
            OaPurchaseItem item = new OaPurchaseItem();
            item.setId(iid);
            item.setPurchaseId(purchaseId);
            item.setItemName(it.itemName());
            item.setSpecification(it.specification());
            item.setQuantity(it.quantity());
            item.setUnit(it.unit());
            item.setUnitPrice(it.unitPrice());
            item.setAmount(it.amount());
            item.setSortOrder(it.sortOrder());
            purchaseItemMapper.insert(item);
        }
    }

    private static BigDecimal sumItems(List<PurchaseDtos.PurchaseItemRequest> items) {
        BigDecimal s = BigDecimal.ZERO;
        for (PurchaseDtos.PurchaseItemRequest it : items) {
            s = s.add(it.amount());
        }
        return s;
    }

    private void assertOwner(Map<String, Object> row) {
        AuthUser user = authService.currentUser();
        long owner = ((Number) row.get("createdBy")).longValue();
        if (!user.permissions().contains("*") && user.id() != owner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此采购申请");
        }
    }

    private void assertAdminOnly() {
        AuthUser user = authService.currentUser();
        if (user.permissions().contains("*") || user.permissions().contains("purchase:manage")) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "仅采购管理员可执行此操作");
    }

    private void assertViewAllowed(Map<String, Object> row) {
        assertOwner(row);
    }

    private Map<String, String> loadUserDeptSnapshot(long userId) {
        Map<String, Object> r = userMapper.selectUserDeptSnapshot(userId);
        if (r == null) {
            return Map.of("deptId", "", "deptName", "");
        }
        Map<String, String> m = new LinkedHashMap<>();
        m.put("deptName", r.get("deptName") == null ? "" : String.valueOf(r.get("deptName")));
        if (r.get("deptId") != null) {
            m.put("deptId", String.valueOf(((Number) r.get("deptId")).longValue()));
        } else {
            m.put("deptId", "");
        }
        return m;
    }

    private static String formatPurchaseNo(long id) {
        return "CG" + String.format("%012d", id);
    }

    private long[] clampPage(long page, long size) {
        int def = intConfig("paging.defaultSize", 20);
        int max = intConfig("paging.maxSize", 100);
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? def : size;
        if (s > max) {
            s = max;
        }
        return new long[]{p, s};
    }

    private int intConfig(String key, int defaultValue) {
        LambdaQueryWrapper<SysConfig> qw = new LambdaQueryWrapper<>();
        qw.select(SysConfig::getConfigValue).eq(SysConfig::getConfigKey, key);
        List<SysConfig> configs = sysConfigMapper.selectList(qw);
        if (configs.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(configs.get(0).getConfigValue());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }
}
