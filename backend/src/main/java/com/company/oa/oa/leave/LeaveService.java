package com.company.oa.oa.leave;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.system.WorkCalendarService;
import com.company.oa.workflow.WorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
@Service
public class LeaveService {
    private static final String DRAFT = "DRAFT";
    private static final String APPROVING = "APPROVING";
    private static final String CANCELLED = "CANCELLED";

    private final OaLeaveMapper oaLeaveMapper;
    private final SysConfigMapper sysConfigMapper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final WorkCalendarService workCalendarService;
    private final SequenceService sequenceService;

    public LeaveService(OaLeaveMapper oaLeaveMapper, SysConfigMapper sysConfigMapper,
                        AuthService authService, WorkflowService workflowService,
                        AuditService auditService, WorkCalendarService workCalendarService,
                        SequenceService sequenceService) {
        this.oaLeaveMapper = oaLeaveMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.authService = authService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.workCalendarService = workCalendarService;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Long applicantId) {
        AuthUser user = authService.currentUser();
        long[] ps = clampPage(page, size);
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaLeave::getDeleted, 0);
        if (applicantId != null) {
            if (!user.permissions().contains("*") && !user.id().equals(applicantId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人请假");
            }
            wrapper.eq(OaLeave::getCreatedBy, applicantId);
        } else {
            wrapper.eq(OaLeave::getCreatedBy, user.id());
        }
        long total = Objects.requireNonNullElse(oaLeaveMapper.selectCount(wrapper), 0L);
        wrapper.orderByDesc(OaLeave::getId);
        wrapper.last("limit " + ps[1] + " offset " + (ps[0] - 1) * ps[1]);
        List<OaLeave> entities = oaLeaveMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OaLeave e : entities) {
            items.add(entityToMap(e));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        Map<String, Object> row = loadLeave(id);
        assertViewAllowed(row);
        return row;
    }

    @Transactional
    public Map<String, Object> create(LeaveDtos.LeaveCreateRequest req) {
        AuthUser user = authService.currentUser();
        long newId = sequenceService.nextId("oa_leave");
        Map<String, String> snap = loadUserDeptSnapshot(user.id());
        OaLeave entity = new OaLeave();
        entity.setId(newId);
        entity.setLeaveType(req.leaveType());
        entity.setStartAt(req.startAt());
        entity.setEndAt(req.endAt());
        entity.setDurationHours(req.durationHours());
        entity.setDurationDays(req.durationDays());
        entity.setReason(req.reason());
        entity.setHandoverNote(req.handoverNote());
        entity.setStatus(DRAFT);
        entity.setCreatedBy(user.id());
        entity.setCreatedNameSnapshot(user.realName());
        entity.setCreatedDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty()
                ? null : Long.parseLong(snap.get("deptId")));
        entity.setCreatedDeptNameSnapshot(snap.get("deptName"));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        oaLeaveMapper.insert(entity);
        return detail(newId);
    }

    @Transactional
    public Map<String, Object> update(long id, LeaveDtos.LeaveUpdateRequest req) {
        Map<String, Object> row = loadLeave(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可编辑");
        }
        OaLeave entity = new OaLeave();
        entity.setLeaveType(req.leaveType());
        entity.setStartAt(req.startAt());
        entity.setEndAt(req.endAt());
        entity.setDurationHours(req.durationHours());
        entity.setDurationDays(req.durationDays());
        entity.setReason(req.reason());
        entity.setHandoverNote(req.handoverNote());
        oaLeaveMapper.update(entity, new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getId, id)
                .eq(OaLeave::getDeleted, 0));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> submit(long id) {
        Map<String, Object> row = loadLeave(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可提交");
        }
        String title = "请假申请-" + row.get("leaveType") + "-" + id;
        Map<String, Object> wf = workflowService.startInstance(new WorkflowDtos.StartInstanceRequest(
                "LEAVE",
                id,
                title,
                Map.of("durationDays", row.get("durationDays"))
        ));
        OaLeave entity = new OaLeave();
        entity.setStatus(APPROVING);
        entity.setProcessInstanceId((String) wf.get("processInstanceId"));
        entity.setWfInstanceId(toLong(wf.get("wfInstanceId")));
        entity.setUpdatedAt(LocalDateTime.now());
        oaLeaveMapper.update(entity, new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getId, id)
                .eq(OaLeave::getDeleted, 0));
        Map<String, Object> out = detail(id);
        out.put("currentNodeName", wf.get("currentNodeName"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "LEAVE_SUBMIT", "LEAVE", id, AuditService.SUCCESS, null);
        return out;
    }

    @Transactional
    public Map<String, Object> withdrawLeave(long id) {
        Map<String, Object> row = loadLeave(id);
        assertOwner(row);
        if (!APPROVING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审批中可撤回");
        }
        Object wfInst = row.get("wfInstanceId");
        if (wfInst == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未关联流程实例");
        }
        long wfInstanceId = ((Number) wfInst).longValue();
        workflowService.withdrawInstance(wfInstanceId);
        oaLeaveMapper.updateStatusClearFlowKeysById(id, "WITHDRAWN", LocalDateTime.now());
        auditService.safeRecordOperation(authService.currentUser().id(),
                "LEAVE_WITHDRAW", "LEAVE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancelLeave(long id) {
        Map<String, Object> row = loadLeave(id);
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
            oaLeaveMapper.updateStatusById(id, CANCELLED, LocalDateTime.now());
        }
        auditService.safeRecordOperation(authService.currentUser().id(),
                "LEAVE_CANCEL", "LEAVE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculateDuration(String startAt, String endAt) {
        LocalDateTime start = LocalDateTime.parse(startAt);
        LocalDateTime end = LocalDateTime.parse(endAt);
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        int workdays = workCalendarService.countWorkdays(startDate, endDate);
        double hours = workdays * 8.0;
        double days = (double) workdays;
        return Map.of(
                "startAt", startAt,
                "endAt", endAt,
                "durationHours", hours,
                "durationDays", days
        );
    }

    private Map<String, Object> loadLeave(long id) {
        OaLeave entity = oaLeaveMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "请假单不存在");
        }
        return entityToMap(entity);
    }

    private void assertOwner(Map<String, Object> row) {
        AuthUser user = authService.currentUser();
        long owner = ((Number) row.get("createdBy")).longValue();
        if (!user.permissions().contains("*") && !user.id().equals(owner)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此请假单");
        }
    }

    private void assertViewAllowed(Map<String, Object> row) {
        assertOwner(row);
    }

    private Map<String, String> loadUserDeptSnapshot(long userId) {
        Map<String, Object> r = oaLeaveMapper.selectUserDeptSnapshot(userId);
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

    private Map<String, Object> entityToMap(OaLeave entity) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("processInstanceId", entity.getProcessInstanceId());
        map.put("wfInstanceId", entity.getWfInstanceId());
        map.put("ruleVersionId", entity.getRuleVersionId());
        map.put("leaveType", entity.getLeaveType());
        map.put("startAt", entity.getStartAt());
        map.put("endAt", entity.getEndAt());
        map.put("durationHours", entity.getDurationHours());
        map.put("durationDays", entity.getDurationDays());
        map.put("reason", entity.getReason());
        map.put("handoverNote", entity.getHandoverNote());
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
