package com.company.oa.oa.seal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.oa.OaSealApply;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.oa.mapper.OaSealApplyMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.workflow.WorkflowService;
import com.company.oa.common.service.SequenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SealService {
    private static final String DRAFT = "DRAFT";
    private static final String APPROVING = "APPROVING";
    private static final String CANCELLED = "CANCELLED";
    private static final String APPROVED = "APPROVED";

    private final OaSealApplyMapper oaSealApplyMapper;
    private final SysConfigMapper sysConfigMapper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final SequenceService sequenceService;

    public SealService(OaSealApplyMapper oaSealApplyMapper, SysConfigMapper sysConfigMapper,
                       AuthService authService, WorkflowService workflowService,
                       AuditService auditService, SequenceService sequenceService) {
        this.oaSealApplyMapper = oaSealApplyMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.authService = authService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Long applicantId) {
        AuthUser user = authService.currentUser();
        long[] ps = clampPage(page, size);
        LambdaQueryWrapper<OaSealApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaSealApply::getDeleted, 0);
        if (applicantId != null) {
            if (!user.permissions().contains("*") && !user.id().equals(applicantId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人用章申请");
            }
            wrapper.eq(OaSealApply::getCreatedBy, applicantId);
        } else {
            wrapper.eq(OaSealApply::getCreatedBy, user.id());
        }
        long total = Objects.requireNonNullElse(oaSealApplyMapper.selectCount(wrapper), 0L);
        wrapper.orderByDesc(OaSealApply::getId);
        wrapper.last("limit " + ps[1] + " offset " + (ps[0] - 1) * ps[1]);
        List<OaSealApply> entities = oaSealApplyMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OaSealApply e : entities) {
            items.add(entityToMap(e));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        Map<String, Object> row = loadSeal(id);
        assertViewAllowed(row);
        return row;
    }

    @Transactional
    public Map<String, Object> create(SealDtos.SealCreateRequest req) {
        AuthUser user = authService.currentUser();
        long newId = sequenceService.nextId("oa_seal_apply");
        Map<String, String> snap = loadUserDeptSnapshot(user.id());
        OaSealApply entity = new OaSealApply();
        entity.setId(newId);
        entity.setSealType(req.sealType());
        entity.setSealName(req.sealName());
        entity.setFileTitle(req.fileTitle());
        entity.setUseReason(req.useReason());
        entity.setUseAt(req.useAt());
        entity.setOutFlag(req.outFlag());
        entity.setStatus(DRAFT);
        entity.setCreatedBy(user.id());
        entity.setCreatedNameSnapshot(user.realName());
        entity.setCreatedDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty()
                ? null : Long.parseLong(snap.get("deptId")));
        entity.setCreatedDeptNameSnapshot(snap.get("deptName"));
        oaSealApplyMapper.insert(entity);
        return detail(newId);
    }

    @Transactional
    public Map<String, Object> update(long id, SealDtos.SealUpdateRequest req) {
        Map<String, Object> row = loadSeal(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可编辑");
        }
        OaSealApply entity = new OaSealApply();
        entity.setSealType(req.sealType());
        entity.setSealName(req.sealName());
        entity.setFileTitle(req.fileTitle());
        entity.setUseReason(req.useReason());
        entity.setUseAt(req.useAt());
        entity.setOutFlag(req.outFlag());
        oaSealApplyMapper.update(entity, new LambdaQueryWrapper<OaSealApply>()
                .eq(OaSealApply::getId, id)
                .eq(OaSealApply::getDeleted, 0));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> submit(long id) {
        Map<String, Object> row = loadSeal(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可提交");
        }
        String title = "用章申请-" + row.get("fileTitle") + "-" + id;
        String sealImportance = Integer.valueOf(1).equals(row.get("outFlag")) ? "IMPORTANT" : "NORMAL";
        Map<String, Object> wf = workflowService.startInstance(new WorkflowDtos.StartInstanceRequest(
                "SEAL",
                id,
                title,
                Map.of("sealImportance", sealImportance)
        ));
        OaSealApply entity = new OaSealApply();
        entity.setStatus(APPROVING);
        entity.setProcessInstanceId((String) wf.get("processInstanceId"));
        entity.setWfInstanceId(toLong(wf.get("wfInstanceId")));
        oaSealApplyMapper.update(entity, new LambdaQueryWrapper<OaSealApply>()
                .eq(OaSealApply::getId, id)
                .eq(OaSealApply::getDeleted, 0));
        Map<String, Object> out = detail(id);
        out.put("currentNodeName", wf.get("currentNodeName"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "SEAL_SUBMIT", "SEAL", id, AuditService.SUCCESS, null);
        return out;
    }

    @Transactional
    public Map<String, Object> withdrawSeal(long id) {
        Map<String, Object> row = loadSeal(id);
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
                "SEAL_WITHDRAW", "SEAL", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancelSeal(long id) {
        Map<String, Object> row = loadSeal(id);
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
            oaSealApplyMapper.updateStatusById(id, CANCELLED, LocalDateTime.now());
        }
        auditService.safeRecordOperation(authService.currentUser().id(),
                "SEAL_CANCEL", "SEAL", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> returnSeal(long id) {
        Map<String, Object> row = loadSeal(id);
        assertOwner(row);
        if (!APPROVED.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已通过可登记归还");
        }
        int out = ((Number) row.get("outFlag")).intValue();
        if (out != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非外带用章无需归还登记");
        }
        OaSealApply entity = new OaSealApply();
        entity.setReturnAt(LocalDateTime.now());
        oaSealApplyMapper.update(entity, new LambdaQueryWrapper<OaSealApply>()
                .eq(OaSealApply::getId, id)
                .eq(OaSealApply::getDeleted, 0));
        return detail(id);
    }

    private Map<String, Object> loadSeal(long id) {
        OaSealApply entity = oaSealApplyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用章申请不存在");
        }
        return entityToMap(entity);
    }

    private void assertOwner(Map<String, Object> row) {
        AuthUser user = authService.currentUser();
        long owner = ((Number) row.get("createdBy")).longValue();
        if (!user.permissions().contains("*") && user.id() != owner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此用章申请");
        }
    }

    private void assertViewAllowed(Map<String, Object> row) {
        assertOwner(row);
    }

    private Map<String, String> loadUserDeptSnapshot(long userId) {
        Map<String, Object> r = oaSealApplyMapper.selectUserDeptSnapshot(userId);
        Map<String, String> m = new LinkedHashMap<>();
        if (r == null || r.isEmpty()) {
            m.put("deptId", "");
            m.put("deptName", "");
            return m;
        }
        m.put("deptName", r.get("deptName") == null ? "" : String.valueOf(r.get("deptName")));
        if (r.get("deptId") != null) {
            m.put("deptId", String.valueOf(((Number) r.get("deptId")).longValue()));
        } else {
            m.put("deptId", "");
        }
        return m;
    }

    private Map<String, Object> entityToMap(OaSealApply entity) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("processInstanceId", entity.getProcessInstanceId());
        map.put("wfInstanceId", entity.getWfInstanceId());
        map.put("sealType", entity.getSealType());
        map.put("sealName", entity.getSealName());
        map.put("fileTitle", entity.getFileTitle());
        map.put("useReason", entity.getUseReason());
        map.put("useAt", entity.getUseAt());
        map.put("outFlag", entity.getOutFlag());
        map.put("returnAt", entity.getReturnAt());
        map.put("status", entity.getStatus());
        map.put("createdBy", entity.getCreatedBy());
        map.put("createdName", entity.getCreatedNameSnapshot());
        map.put("createdDeptId", entity.getCreatedDeptId());
        map.put("createdDeptName", entity.getCreatedDeptNameSnapshot());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        map.put("version", entity.getVersion());
        return map;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        return ((Number) value).longValue();
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
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return Integer.parseInt(config.getConfigValue());
    }
}
