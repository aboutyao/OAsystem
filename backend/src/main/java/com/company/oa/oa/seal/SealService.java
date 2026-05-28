package com.company.oa.oa.seal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.service.OaSnapshotUtils;
import com.company.oa.common.service.OaUtils;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.oa.OaSealApply;
import com.company.oa.oa.mapper.OaSealApplyMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.workflow.WorkflowService;
import com.company.oa.common.service.SequenceService;
import com.company.oa.common.service.OaPermissionUtils;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final PaginationHelper paginationHelper;
    private final AuthService authService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final UserMapper userMapper;

    public SealService(OaSealApplyMapper oaSealApplyMapper, PaginationHelper paginationHelper,
                       AuthService authService, WorkflowService workflowService,
                       AuditService auditService, SequenceService sequenceService,
                       UserMapper userMapper) {
        this.oaSealApplyMapper = oaSealApplyMapper;
        this.paginationHelper = paginationHelper;
        this.authService = authService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Long applicantId) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
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
        OaPermissionUtils.assertViewAllowed(row, authService, "此记录");
        return row;
    }

    @Transactional
    public Map<String, Object> create(SealDtos.SealCreateRequest req) {
        AuthUser user = authService.currentUser();
        long newId = sequenceService.nextId("oa_seal_apply");
        Map<String, String> snap = OaSnapshotUtils.loadUserDeptSnapshot(user.id(), userMapper);
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
        OaPermissionUtils.assertOwner(row, authService, "此记录");
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
        OaPermissionUtils.assertOwner(row, authService, "此记录");
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
        entity.setWfInstanceId(OaUtils.toLong(wf.get("wfInstanceId")));
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
                "SEAL_WITHDRAW", "SEAL", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> cancelSeal(long id) {
        Map<String, Object> row = loadSeal(id);
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
            oaSealApplyMapper.updateStatusById(id, CANCELLED, LocalDateTime.now());
        }
        auditService.safeRecordOperation(authService.currentUser().id(),
                "SEAL_CANCEL", "SEAL", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> returnSeal(long id) {
        Map<String, Object> row = loadSeal(id);
        OaPermissionUtils.assertOwner(row, authService, "此记录");
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

    @Transactional(readOnly = true)
    public void exportSeals(Map<String, Object> filter, HttpServletResponse response) {
        AuthUser user = authService.currentUser();
        LambdaQueryWrapper<OaSealApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaSealApply::getDeleted, 0);
        if (user.permissions().contains("*") && filter != null && filter.containsKey("applicantId")) {
            wrapper.eq(OaSealApply::getCreatedBy, ((Number) filter.get("applicantId")).longValue());
        } else {
            wrapper.eq(OaSealApply::getCreatedBy, user.id());
        }
        if (filter != null && filter.containsKey("status")) {
            wrapper.eq(OaSealApply::getStatus, String.valueOf(filter.get("status")));
        }
        wrapper.orderByDesc(OaSealApply::getId);
        List<OaSealApply> entities = oaSealApplyMapper.selectList(wrapper);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (OaSealApply e : entities) {
            rows.add(entityToMap(e));
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("用章申请列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream())
                    .head(List.of(
                            List.of("编号", "印章类型", "印章名称", "文件标题", "用章事由",
                                    "用章时间", "是否外带", "归还时间", "状态",
                                    "申请人", "部门", "创建时间")
                    ))
                    .sheet("用章申请列表")
                    .doWrite(rows.stream().map(r -> List.of(
                            String.valueOf(r.get("id")),
                            String.valueOf(r.get("sealType")),
                            String.valueOf(r.get("sealName")),
                            String.valueOf(r.get("fileTitle")),
                            String.valueOf(r.get("useReason")),
                            String.valueOf(r.get("useAt")),
                            "1".equals(String.valueOf(r.get("outFlag"))) ? "是" : "否",
                            String.valueOf(r.get("returnAt")),
                            String.valueOf(r.get("status")),
                            String.valueOf(r.get("createdName")),
                            String.valueOf(r.get("createdDeptName")),
                            String.valueOf(r.get("createdAt"))
                    )).toList());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private Map<String, Object> loadSeal(long id) {
        OaSealApply entity = oaSealApplyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用章申请不存在");
        }
        return entityToMap(entity);
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

}
