package com.company.oa.oa.leave;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alibaba.excel.EasyExcel;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.OaSnapshotUtils;
import com.company.oa.common.service.OaUtils;
import com.company.oa.common.service.OaPermissionUtils;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.system.WorkCalendarService;
import com.company.oa.workflow.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final PaginationHelper paginationHelper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final WorkCalendarService workCalendarService;
    private final SequenceService sequenceService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public LeaveService(OaLeaveMapper oaLeaveMapper, PaginationHelper paginationHelper,
                        AuthService authService, WorkflowService workflowService,
                        AuditService auditService, WorkCalendarService workCalendarService,
                        SequenceService sequenceService, UserMapper userMapper,
                        ObjectMapper objectMapper) {
        this.oaLeaveMapper = oaLeaveMapper;
        this.paginationHelper = paginationHelper;
        this.authService = authService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.workCalendarService = workCalendarService;
        this.sequenceService = sequenceService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Long applicantId) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
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
        OaPermissionUtils.assertViewAllowed(row, authService, "此记录");
        return row;
    }

    @Transactional
    public Map<String, Object> create(LeaveDtos.LeaveCreateRequest req) {
        AuthUser user = authService.currentUser();
        long newId = sequenceService.nextId("oa_leave");
        Map<String, String> snap = OaSnapshotUtils.loadUserDeptSnapshot(user.id(), userMapper);
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
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可编辑");
        }
        // Capture old values for audit diff
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("leaveType", row.get("leaveType"));
        oldValues.put("startAt", row.get("startAt"));
        oldValues.put("endAt", row.get("endAt"));
        oldValues.put("durationHours", row.get("durationHours"));
        oldValues.put("durationDays", row.get("durationDays"));
        oldValues.put("reason", row.get("reason"));
        oldValues.put("handoverNote", row.get("handoverNote"));

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

        // Capture new values and record audit diff
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("leaveType", req.leaveType());
        newValues.put("startAt", req.startAt());
        newValues.put("endAt", req.endAt());
        newValues.put("durationHours", req.durationHours());
        newValues.put("durationDays", req.durationDays());
        newValues.put("reason", req.reason());
        newValues.put("handoverNote", req.handoverNote());
        auditService.recordUpdate(authService.currentUser().id(), "LEAVE", id, oldValues, newValues);

        return detail(id);
    }

    @Transactional
    public Map<String, Object> submit(long id) {
        Map<String, Object> row = loadLeave(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可提交");
        }
        String oldStatus = String.valueOf(row.get("status"));
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
        entity.setWfInstanceId(OaUtils.toLong(wf.get("wfInstanceId")));
        entity.setUpdatedAt(LocalDateTime.now());
        oaLeaveMapper.update(entity, new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getId, id)
                .eq(OaLeave::getDeleted, 0));
        Map<String, Object> out = detail(id);
        out.put("currentNodeName", wf.get("currentNodeName"));
        // Record audit with status diff
        Map<String, Object> oldValues = Map.of("status", oldStatus);
        Map<String, Object> newValues = Map.of("status", APPROVING);
        auditService.recordUpdate(authService.currentUser().id(), "LEAVE", id, oldValues, newValues);
        return out;
    }

    @Transactional
    public Map<String, Object> withdrawLeave(long id) {
        Map<String, Object> row = loadLeave(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        if (!APPROVING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审批中可撤回");
        }
        String oldStatus = String.valueOf(row.get("status"));
        Object wfInst = row.get("wfInstanceId");
        if (wfInst == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未关联流程实例");
        }
        long wfInstanceId = ((Number) wfInst).longValue();
        workflowService.withdrawInstance(wfInstanceId);
        oaLeaveMapper.updateStatusClearFlowKeysById(id, "WITHDRAWN", LocalDateTime.now());
        // Record audit with status diff
        Map<String, Object> oldValues = Map.of("status", oldStatus);
        Map<String, Object> newValues = Map.of("status", "WITHDRAWN");
        auditService.recordUpdate(authService.currentUser().id(), "LEAVE", id, oldValues, newValues);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancelLeave(long id) {
        Map<String, Object> row = loadLeave(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
        String st = String.valueOf(row.get("status"));
        if (!DRAFT.equals(st) && !APPROVING.equals(st)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前状态不可作废");
        }
        String oldStatus = st;
        if (APPROVING.equals(st)) {
            Object wfInst = row.get("wfInstanceId");
            if (wfInst != null) {
                workflowService.terminateInstance(((Number) wfInst).longValue());
            }
        } else {
            oaLeaveMapper.updateStatusById(id, CANCELLED, LocalDateTime.now());
        }
        // Record audit with status diff
        Map<String, Object> oldValues = Map.of("status", oldStatus);
        Map<String, Object> newValues = Map.of("status", CANCELLED);
        auditService.recordUpdate(authService.currentUser().id(), "LEAVE", id, oldValues, newValues);
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> teamLeaveCalendar(LocalDate start, LocalDate end) {
        // Query approved leaves overlapping the date range
        // overlap condition: leave.startAt <= end AND leave.endAt >= start
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaLeave::getDeleted, 0);
        wrapper.eq(OaLeave::getStatus, "APPROVED");
        wrapper.le(OaLeave::getStartAt, end.atStartOfDay().plusDays(1).minusNanos(1));
        wrapper.ge(OaLeave::getEndAt, start.atStartOfDay());
        List<OaLeave> leaves = oaLeaveMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (OaLeave e : leaves) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", e.getCreatedBy());
            item.put("userName", e.getCreatedNameSnapshot());
            item.put("leaveType", e.getLeaveType());
            item.put("startAt", e.getStartAt());
            item.put("endAt", e.getEndAt());
            item.put("durationDays", e.getDurationDays());
            result.add(item);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public void exportLeaves(Map<String, Object> filter, HttpServletResponse response) {
        AuthUser user = authService.currentUser();
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaLeave::getDeleted, 0);
        if (user.permissions().contains("*") && filter != null && filter.containsKey("applicantId")) {
            wrapper.eq(OaLeave::getCreatedBy, ((Number) filter.get("applicantId")).longValue());
        } else {
            wrapper.eq(OaLeave::getCreatedBy, user.id());
        }
        if (filter != null && filter.containsKey("status")) {
            wrapper.eq(OaLeave::getStatus, String.valueOf(filter.get("status")));
        }
        wrapper.orderByDesc(OaLeave::getId);
        List<OaLeave> entities = oaLeaveMapper.selectList(wrapper);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (OaLeave e : entities) {
            rows.add(entityToMap(e));
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("请假列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream())
                    .head(List.of(
                            List.of("编号", "类型", "开始时间", "结束时间", "时长(天)", "事由", "状态", "申请人", "部门", "创建时间")
                    ))
                    .sheet("请假列表")
                    .doWrite(rows.stream().map(r -> List.of(
                            String.valueOf(r.get("id")),
                            String.valueOf(r.get("leaveType")),
                            String.valueOf(r.get("startAt")),
                            String.valueOf(r.get("endAt")),
                            String.valueOf(r.get("durationDays")),
                            String.valueOf(r.get("reason")),
                            String.valueOf(r.get("status")),
                            String.valueOf(r.get("createdName")),
                            String.valueOf(r.get("createdDeptName")),
                            String.valueOf(r.get("createdAt"))
                    )).toList());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private Map<String, Object> loadLeave(long id) {
        OaLeave entity = oaLeaveMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "请假单不存在");
        }
        return entityToMap(entity);
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

}
