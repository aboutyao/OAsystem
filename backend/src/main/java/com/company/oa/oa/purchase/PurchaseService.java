package com.company.oa.oa.purchase;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.service.OaSnapshotUtils;
import com.company.oa.common.service.OaUtils;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.entity.oa.OaPurchaseItem;
import com.company.oa.oa.mapper.OaPurchaseItemMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.workflow.WorkflowService;
import com.company.oa.common.service.SequenceService;
import com.company.oa.common.service.OaPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final PaginationHelper paginationHelper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public PurchaseService(
            OaPurchaseMapper purchaseMapper,
            OaPurchaseItemMapper purchaseItemMapper,
            PaginationHelper paginationHelper,
            UserMapper userMapper,
            AuthService authService,
            WorkflowService workflowService,
            AuditService auditService,
            SequenceService sequenceService,
            ObjectMapper objectMapper
    ) {
        this.purchaseMapper = purchaseMapper;
        this.purchaseItemMapper = purchaseItemMapper;
        this.paginationHelper = paginationHelper;
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
        long[] ps = paginationHelper.clamp(page, size);
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
        OaPermissionUtils.assertViewAllowed(row, authService, "此记录");
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
        Map<String, String> snap = OaSnapshotUtils.loadUserDeptSnapshot(user.id(), userMapper);
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
        OaPermissionUtils.assertOwner(row, authService, "此记录");
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
        OaPermissionUtils.assertOwner(row, authService, "此记录");
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
                .set(OaPurchase::getWfInstanceId, OaUtils.toLong(wf.get("wfInstanceId")))
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
        OaPermissionUtils.assertOwner(row, authService, "此记录");
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
        OaPermissionUtils.assertOwner(row, authService, "此记录");
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

    @Transactional(readOnly = true)
    public void exportPurchases(Map<String, Object> filter, HttpServletResponse response) {
        AuthUser user = authService.currentUser();
        LambdaQueryWrapper<OaPurchase> qw = new LambdaQueryWrapper<>();
        if (user.permissions().contains("*") && filter != null && filter.containsKey("applicantId")) {
            qw.eq(OaPurchase::getCreatedBy, ((Number) filter.get("applicantId")).longValue());
        } else {
            qw.eq(OaPurchase::getCreatedBy, user.id());
        }
        if (filter != null && filter.containsKey("status")) {
            qw.eq(OaPurchase::getStatus, String.valueOf(filter.get("status")));
        }
        qw.select(OaPurchase::getId, OaPurchase::getPurchaseNo, OaPurchase::getPurchaseType,
                        OaPurchase::getSupplierName, OaPurchase::getBudgetSubject,
                        OaPurchase::getTotalAmount, OaPurchase::getArrivalStatus,
                        OaPurchase::getAcceptanceStatus, OaPurchase::getStatus,
                        OaPurchase::getCreatedBy, OaPurchase::getCreatedNameSnapshot,
                        OaPurchase::getCreatedDeptNameSnapshot, OaPurchase::getCreatedAt)
                .orderByDesc(OaPurchase::getId);
        List<OaPurchase> entities = purchaseMapper.selectList(qw);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (OaPurchase e : entities) {
            rows.add(toMap(e));
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("采购申请列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream())
                    .head(List.of(
                            List.of("编号", "采购单号", "采购类型", "供应商", "预算科目",
                                    "总金额", "到货状态", "验收状态", "状态",
                                    "申请人", "部门", "创建时间")
                    ))
                    .sheet("采购申请列表")
                    .doWrite(rows.stream().map(r -> List.of(
                            String.valueOf(r.get("id")),
                            String.valueOf(r.get("purchaseNo")),
                            String.valueOf(r.get("purchaseType")),
                            String.valueOf(r.get("supplierName")),
                            String.valueOf(r.get("budgetSubject")),
                            String.valueOf(r.get("totalAmount")),
                            String.valueOf(r.get("arrivalStatus")),
                            String.valueOf(r.get("acceptanceStatus")),
                            String.valueOf(r.get("status")),
                            String.valueOf(r.get("createdNameSnapshot")),
                            String.valueOf(r.get("createdDeptNameSnapshot")),
                            String.valueOf(r.get("createdAt"))
                    )).toList());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
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


    private void assertAdminOnly() {
        AuthUser user = authService.currentUser();
        if (user.permissions().contains("*") || user.permissions().contains("purchase:manage")) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "仅采购管理员可执行此操作");
    }


    private static String formatPurchaseNo(long id) {
        return "CG" + String.format("%012d", id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }

}
