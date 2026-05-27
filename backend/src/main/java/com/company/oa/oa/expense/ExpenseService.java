package com.company.oa.oa.expense;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.oa.OaExpense;
import com.company.oa.entity.oa.OaExpenseItem;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.oa.mapper.OaExpenseItemMapper;
import com.company.oa.oa.mapper.OaExpenseMapper;
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
public class ExpenseService {
    private static final String DRAFT = "DRAFT";
    private static final String APPROVING = "APPROVING";
    private static final String CANCELLED = "CANCELLED";
    private static final String APPROVED = "APPROVED";
    private static final String UNPAID = "UNPAID";
    private static final String PAID = "PAID";

    private final OaExpenseMapper expenseMapper;
    private final OaExpenseItemMapper expenseItemMapper;
    private final SysConfigMapper sysConfigMapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public ExpenseService(
            OaExpenseMapper expenseMapper,
            OaExpenseItemMapper expenseItemMapper,
            SysConfigMapper sysConfigMapper,
            UserMapper userMapper,
            AuthService authService,
            WorkflowService workflowService,
            AuditService auditService,
            SequenceService sequenceService,
            ObjectMapper objectMapper
    ) {
        this.expenseMapper = expenseMapper;
        this.expenseItemMapper = expenseItemMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.userMapper = userMapper;
        this.authService = authService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Long applicantId) {
        AuthUser user = authService.currentUser();
        long[] ps = clampPage(page, size);
        LambdaQueryWrapper<OaExpense> qw = new LambdaQueryWrapper<>();
        if (applicantId != null) {
            if (!user.permissions().contains("*") && !user.id().equals(applicantId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人报销");
            }
            qw.eq(OaExpense::getCreatedBy, applicantId);
        } else {
            qw.eq(OaExpense::getCreatedBy, user.id());
        }
        long total = expenseMapper.selectCount(qw);
        qw.select(OaExpense::getId, OaExpense::getExpenseNo, OaExpense::getExpenseType,
                        OaExpense::getTotalAmount, OaExpense::getPaymentStatus, OaExpense::getStatus,
                        OaExpense::getProcessInstanceId, OaExpense::getWfInstanceId,
                        OaExpense::getCreatedAt, OaExpense::getUpdatedAt)
                .orderByDesc(OaExpense::getId)
                .last("limit " + ps[1] + " offset " + ((ps[0] - 1) * ps[1]));
        List<OaExpense> expenses = expenseMapper.selectList(qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OaExpense e : expenses) {
            items.add(toMap(e));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        Map<String, Object> row = loadExpenseHeader(id);
        assertViewAllowed(row);
        row.put("items", loadExpenseItems(id));
        return row;
    }

    @Transactional
    public Map<String, Object> create(ExpenseDtos.ExpenseCreateRequest req) {
        BigDecimal sum = sumItems(req.items());
        if (req.totalAmount().compareTo(sum) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请金额与明细合计不一致");
        }
        AuthUser user = authService.currentUser();
        long newId = sequenceService.nextId("oa_expense");
        String expenseNo = formatExpenseNo(newId);
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> snap = loadUserDeptSnapshot(user.id());
        OaExpense entity = new OaExpense();
        entity.setId(newId);
        entity.setExpenseNo(expenseNo);
        entity.setExpenseType(req.expenseType());
        entity.setTotalAmount(req.totalAmount());
        entity.setPayeeAccount(req.payeeAccount());
        entity.setPaymentStatus(UNPAID);
        entity.setReason(req.reason());
        entity.setStatus(DRAFT);
        entity.setCreatedBy(user.id());
        entity.setCreatedNameSnapshot(user.realName());
        entity.setCreatedDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty() ? null : Long.parseLong(snap.get("deptId")));
        entity.setCreatedDeptNameSnapshot(snap.get("deptName"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        expenseMapper.insert(entity);
        replaceItems(newId, req.items());
        return detail(newId);
    }

    @Transactional
    public Map<String, Object> update(long id, ExpenseDtos.ExpenseUpdateRequest req) {
        Map<String, Object> row = loadExpenseHeader(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可编辑");
        }
        BigDecimal sum = sumItems(req.items());
        if (req.totalAmount().compareTo(sum) != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请金额与明细合计不一致");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaExpense> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaExpense::getId, id)
                .set(OaExpense::getExpenseType, req.expenseType())
                .set(OaExpense::getTotalAmount, req.totalAmount())
                .set(OaExpense::getPayeeAccount, req.payeeAccount())
                .set(OaExpense::getReason, req.reason())
                .set(OaExpense::getUpdatedAt, now)
                .setSql("version = version + 1");
        expenseMapper.update(null, uw);
        replaceItems(id, req.items());
        return detail(id);
    }

    @Transactional
    public Map<String, Object> submit(long id) {
        Map<String, Object> row = loadExpenseHeader(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可提交");
        }
        String expenseNo = String.valueOf(row.get("expenseNo"));
        String title = "报销申请-" + expenseNo;
        Map<String, Object> wf = workflowService.startInstance(new WorkflowDtos.StartInstanceRequest(
                "EXPENSE",
                id,
                title,
                Map.of("totalAmount", row.get("totalAmount"))
        ));
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaExpense> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaExpense::getId, id)
                .set(OaExpense::getStatus, APPROVING)
                .set(OaExpense::getProcessInstanceId, (String) wf.get("processInstanceId"))
                .set(OaExpense::getWfInstanceId, toLong(wf.get("wfInstanceId")))
                .set(OaExpense::getUpdatedAt, now)
                .setSql("version = version + 1");
        expenseMapper.update(null, uw);
        Map<String, Object> out = detail(id);
        out.put("currentNodeName", wf.get("currentNodeName"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "EXPENSE_SUBMIT", "EXPENSE", id, AuditService.SUCCESS, null);
        return out;
    }

    @Transactional
    public Map<String, Object> withdrawExpense(long id) {
        Map<String, Object> row = loadExpenseHeader(id);
        assertOwner(row);
        if (!APPROVING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审批中可撤回");
        }
        Object wfInst = row.get("wfInstanceId");
        if (wfInst == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未关联流程实例");
        }
        workflowService.withdrawInstance(((Number) wfInst).longValue());
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaExpense> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaExpense::getId, id)
                .set(OaExpense::getStatus, "WITHDRAWN")
                .set(OaExpense::getProcessInstanceId, (Long) null)
                .set(OaExpense::getWfInstanceId, (Long) null)
                .set(OaExpense::getUpdatedAt, now)
                .setSql("version = version + 1");
        expenseMapper.update(null, uw);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancelExpense(long id) {
        Map<String, Object> row = loadExpenseHeader(id);
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
            LambdaUpdateWrapper<OaExpense> uw = new LambdaUpdateWrapper<>();
            uw.eq(OaExpense::getId, id)
                    .set(OaExpense::getStatus, CANCELLED)
                    .set(OaExpense::getUpdatedAt, now)
                    .setSql("version = version + 1");
            expenseMapper.update(null, uw);
        }
        return detail(id);
    }

    @Transactional
    public Map<String, Object> markPaid(long id, ExpenseDtos.ExpenseMarkPaidRequest body) {
        Map<String, Object> row = loadExpenseHeader(id);
        assertFinanceOrAdmin();
        if (!APPROVED.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已通过可标记付款");
        }
        if (!UNPAID.equals(String.valueOf(row.get("paymentStatus")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前付款状态不可重复标记");
        }
        BigDecimal total = toBigDecimal(row.get("totalAmount"));
        BigDecimal paid = body != null && body.paidAmount() != null ? body.paidAmount() : total;
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<OaExpense> uw = new LambdaUpdateWrapper<>();
        uw.eq(OaExpense::getId, id)
                .set(OaExpense::getPaymentStatus, PAID)
                .set(OaExpense::getPaidAmount, paid)
                .set(OaExpense::getPaidAt, now)
                .set(OaExpense::getUpdatedAt, now)
                .setSql("version = version + 1");
        expenseMapper.update(null, uw);
        auditService.safeRecordOperation(authService.currentUser().id(),
                "EXPENSE_MARK_PAID", "EXPENSE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> printPayload(long id) {
        Map<String, Object> row = loadExpenseHeader(id);
        assertViewAllowed(row);
        return Map.of(
                "id", id,
                "expenseNo", row.get("expenseNo"),
                "printUrl", "/api/oa/expenses/" + id + "/print"
        );
    }

    private Map<String, Object> loadExpenseHeader(long id) {
        OaExpense entity = expenseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报销单不存在");
        }
        Map<String, Object> map = toMap(entity);
        map.remove("deleted");
        return new LinkedHashMap<>(map);
    }

    private List<Map<String, Object>> loadExpenseItems(long expenseId) {
        LambdaQueryWrapper<OaExpenseItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OaExpenseItem::getExpenseId, expenseId)
                .orderByAsc(OaExpenseItem::getSortOrder)
                .orderByAsc(OaExpenseItem::getId);
        List<OaExpenseItem> items = expenseItemMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (OaExpenseItem item : items) {
            result.add(toMap(item));
        }
        return result;
    }

    private void replaceItems(long expenseId, List<ExpenseDtos.ExpenseItemRequest> items) {
        LambdaQueryWrapper<OaExpenseItem> qw = new LambdaQueryWrapper<>();
        qw.eq(OaExpenseItem::getExpenseId, expenseId);
        expenseItemMapper.delete(qw);
        for (ExpenseDtos.ExpenseItemRequest it : items) {
            long iid = sequenceService.nextId("oa_expense_item");
            OaExpenseItem item = new OaExpenseItem();
            item.setId(iid);
            item.setExpenseId(expenseId);
            item.setFeeType(it.feeType());
            item.setFeeDate(it.feeDate());
            item.setAmount(it.amount());
            item.setDescription(it.description());
            item.setSortOrder(it.sortOrder());
            expenseItemMapper.insert(item);
        }
    }

    private static BigDecimal sumItems(List<ExpenseDtos.ExpenseItemRequest> items) {
        BigDecimal s = BigDecimal.ZERO;
        for (ExpenseDtos.ExpenseItemRequest it : items) {
            s = s.add(it.amount());
        }
        return s;
    }

    private void assertOwner(Map<String, Object> row) {
        AuthUser user = authService.currentUser();
        long owner = ((Number) row.get("createdBy")).longValue();
        if (!user.permissions().contains("*") && !user.id().equals(owner)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此报销单");
        }
    }

    private void assertFinanceOrAdmin() {
        AuthUser user = authService.currentUser();
        if (user.permissions().contains("*") || user.permissions().contains("expense:finance")) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "仅财务管理员可执行此操作");
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

    private static String formatExpenseNo(long id) {
        return "BX" + String.format("%012d", id);
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

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
