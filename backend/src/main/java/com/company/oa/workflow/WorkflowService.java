package com.company.oa.workflow;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.entity.wf.*;
import com.company.oa.message.MessageService;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.oa.mapper.OaSealApplyMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.permission.mapper.PermUserRoleMapper;
import com.company.oa.workflow.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowService {
    private static final String PENDING = "PENDING";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";
    private static final String APPROVING = "APPROVING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String TERMINATED = "TERMINATED";
    private static final String TASK_TYPE_APPROVE = "APPROVE";

    private static final String ADD_SIGN_SEQUENTIAL = "SEQUENTIAL";
    private static final String ADD_SIGN_PARALLEL = "PARALLEL";
    private static final String DELEGATION_ACTIVE = "ACTIVE";
    private static final String DELEGATION_CANCELLED = "CANCELLED";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final AuthService authService;
    private final AuditService auditService;
    private final MessageService messageService;
    private final SequenceService sequenceService;
    private final WfProcessTemplateMapper templateMapper;
    private final WfProcessVersionMapper versionMapper;
    private final WfProcessInstanceMapper instanceMapper;
    private final WfTaskMapper wfTaskMapper;
    private final WfTaskRecordMapper taskRecordMapper;
    private final WfCcRecordMapper ccRecordMapper;
    private final WfDelegationMapper delegationMapper;
    private final UserMapper userMapper;
    private final PermUserRoleMapper userRoleMapper;
    private final PaginationHelper paginationHelper;
    private final OaLeaveMapper leaveMapper;
    private final OaExpenseMapper expenseMapper;
    private final OaSealApplyMapper sealApplyMapper;
    private final OaPurchaseMapper purchaseMapper;
    private final ContractInfoMapper contractMapper;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public WorkflowService(
            RuntimeService runtimeService,
            TaskService taskService,
            RepositoryService repositoryService,
            HistoryService historyService,
            AuthService authService,
            AuditService auditService,
            MessageService messageService,
            SequenceService sequenceService,
            WfProcessTemplateMapper templateMapper,
            WfProcessVersionMapper versionMapper,
            WfProcessInstanceMapper instanceMapper,
            WfTaskMapper wfTaskMapper,
            WfTaskRecordMapper taskRecordMapper,
            WfCcRecordMapper ccRecordMapper,
            WfDelegationMapper delegationMapper,
            UserMapper userMapper,
            PermUserRoleMapper userRoleMapper,
            PaginationHelper paginationHelper,
            OaLeaveMapper leaveMapper,
            OaExpenseMapper expenseMapper,
            OaSealApplyMapper sealApplyMapper,
            OaPurchaseMapper purchaseMapper,
            ContractInfoMapper contractMapper
    ) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.authService = authService;
        this.auditService = auditService;
        this.messageService = messageService;
        this.sequenceService = sequenceService;
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.instanceMapper = instanceMapper;
        this.wfTaskMapper = wfTaskMapper;
        this.taskRecordMapper = taskRecordMapper;
        this.ccRecordMapper = ccRecordMapper;
        this.delegationMapper = delegationMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.paginationHelper = paginationHelper;
        this.leaveMapper = leaveMapper;
        this.expenseMapper = expenseMapper;
        this.sealApplyMapper = sealApplyMapper;
        this.purchaseMapper = purchaseMapper;
        this.contractMapper = contractMapper;
    }

    private record PublishedVersion(long templateId, long versionId, String processDefinitionKey) {
    }

    // ─── Entity → Map helper ───────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }

    // ─── Template CRUD ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listTemplates(long page, long size) {
        long[] ps = paginationHelper.clamp(page, size);
        long total = templateMapper.selectCount(new LambdaQueryWrapper<WfProcessTemplate>());
        List<WfProcessTemplate> items = templateMapper.selectList(
                new LambdaQueryWrapper<WfProcessTemplate>()
                        .orderByAsc(WfProcessTemplate::getId)
                        .last("limit " + ps[1] + " offset " + (ps[0] - 1) * ps[1])
        );
        return new PageResponse<>(ps[0], ps[1], total, items.stream().map(this::toMap).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> templateDetail(long id) {
        WfProcessTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程模板不存在");
        }
        Map<String, Object> m = toMap(t);
        List<WfProcessVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<WfProcessVersion>()
                        .eq(WfProcessVersion::getTemplateId, id)
                        .orderByDesc(WfProcessVersion::getVersionNo)
        );
        m.put("versions", versions.stream().map(this::toMap).collect(Collectors.toList()));
        return m;
    }

    @Transactional
    public Map<String, Object> createTemplate(WorkflowDtos.CreateTemplateRequest req) {
        AuthUser user = authService.currentUser();
        Long dup = templateMapper.selectCount(
                new LambdaQueryWrapper<WfProcessTemplate>().eq(WfProcessTemplate::getTemplateCode, req.templateCode())
        );
        if (dup > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "模板编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        WfProcessTemplate entity = new WfProcessTemplate();
        entity.setId(nextWfId("wf_process_template"));
        entity.setTemplateCode(req.templateCode());
        entity.setTemplateName(req.templateName());
        entity.setBusinessType(req.businessType());
        entity.setDescription(req.description());
        entity.setStatus("ENABLED");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        templateMapper.insert(entity);
        auditService.safeRecordOperation(user.id(), "WF_TEMPLATE_CREATE", "wf_process_template", entity.getId(), AuditService.SUCCESS, null);
        return Map.of("id", entity.getId(), "templateCode", req.templateCode(), "templateName", req.templateName(),
                "businessType", req.businessType(), "description", req.description() == null ? "" : req.description(),
                "status", "ENABLED", "createdAt", now.toString());
    }

    @Transactional
    public Map<String, Object> updateTemplate(long id, WorkflowDtos.UpdateTemplateRequest req) {
        AuthUser user = authService.currentUser();
        WfProcessTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程模板不存在");
        }
        boolean hasUpdate = false;
        WfProcessTemplate upd = new WfProcessTemplate();
        upd.setId(id);
        if (req.templateName() != null) {
            upd.setTemplateName(req.templateName());
            hasUpdate = true;
        }
        if (req.description() != null) {
            upd.setDescription(req.description());
            hasUpdate = true;
        }
        if (req.status() != null) {
            upd.setStatus(req.status());
            hasUpdate = true;
        }
        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "没有可更新的字段");
        }
        upd.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(upd);
        auditService.safeRecordOperation(user.id(), "WF_TEMPLATE_UPDATE", "wf_process_template", id, AuditService.SUCCESS, null);
        return templateDetail(id);
    }

    // ─── Version CRUD ──────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> createVersion(long templateId, WorkflowDtos.CreateVersionRequest req) {
        AuthUser user = authService.currentUser();
        WfProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程模板不存在");
        }
        Long maxVer = versionMapper.selectObjs(
                new LambdaQueryWrapper<WfProcessVersion>()
                        .eq(WfProcessVersion::getTemplateId, templateId)
                        .select(WfProcessVersion::getVersionNo)
                        .orderByDesc(WfProcessVersion::getVersionNo)
                        .last("limit 1")
        ).stream().findFirst().map(v -> ((Number) v).longValue()).orElse(0L);
        long nextVerNo = maxVer + 1;
        LocalDateTime now = LocalDateTime.now();
        WfProcessVersion entity = new WfProcessVersion();
        entity.setId(nextWfId("wf_process_version"));
        entity.setTemplateId(templateId);
        entity.setVersionNo((int) nextVerNo);
        entity.setStatus("DRAFT");
        entity.setChangeReason(req.changeReason());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        versionMapper.insert(entity);
        auditService.safeRecordOperation(user.id(), "WF_VERSION_CREATE", "wf_process_version", entity.getId(), AuditService.SUCCESS, null);
        return Map.of("id", entity.getId(), "templateId", templateId, "versionNo", nextVerNo,
                "status", "DRAFT", "changeReason", req.changeReason() == null ? "" : req.changeReason(),
                "createdAt", now.toString());
    }

    @Transactional
    public Map<String, Object> publishVersion(long versionId) {
        AuthUser user = authService.currentUser();
        WfProcessVersion ver = versionMapper.selectById(versionId);
        if (ver == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程版本不存在");
        }
        if ("PUBLISHED".equals(ver.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该版本已发布");
        }
        if (!"DRAFT".equals(ver.getStatus()) && !"ARCHIVED".equals(ver.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅 DRAFT 或 ARCHIVED 状态可发布");
        }
        long templateId = ver.getTemplateId();
        LocalDateTime now = LocalDateTime.now();

        // Archive the previously published version for this template (if any)
        versionMapper.archivePublished(templateId, now);

        // Deploy to Flowable if bpmn_xml is present
        String flowableDefId = ver.getFlowableDefinitionId();
        String bpmnXml = ver.getBpmnXml();
        if (bpmnXml != null && !bpmnXml.isBlank()) {
            try {
                org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment()
                        .addString("process.bpmn20.xml", bpmnXml)
                        .deploy();
                org.flowable.engine.repository.ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                        .deploymentId(deployment.getId())
                        .singleResult();
                if (pd != null) {
                    flowableDefId = pd.getKey();
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "BPMN 部署失败: " + e.getMessage());
            }
        }

        versionMapper.publish(versionId, user.id(), flowableDefId, now);
        auditService.safeRecordOperation(user.id(), "WF_VERSION_PUBLISH", "wf_process_version", versionId, AuditService.SUCCESS, null);
        return Map.of("id", versionId, "templateId", templateId, "status", "PUBLISHED",
                "publishedAt", now.toString(), "publishedBy", user.id());
    }

    // ─── Instance lifecycle ────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> startInstance(WorkflowDtos.StartInstanceRequest req) {
        AuthUser starter = authService.currentUser();
        assertNoActiveInstance(req.businessType(), req.businessId());

        PublishedVersion ver = resolvePublishedVersion(req.businessType());
        if (repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(ver.processDefinitionKey())
                .latestVersion()
                .count() == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "流程定义未部署，请检查 BPMN 是否已发布到 Flowable");
        }

        Long managerId = resolveManagerId(starter.id(), req.variables());
        Map<String, Object> flowVars = new HashMap<>();
        if (req.variables() != null) {
            for (Map.Entry<String, Object> e : req.variables().entrySet()) {
                if (e.getValue() != null) {
                    flowVars.put(e.getKey(), e.getValue());
                }
            }
        }
        flowVars.put("managerId", String.valueOf(managerId));
        flowVars.put("starterId", starter.id());

        // Resolve role-based assignee IDs for BPMN templates that use them
        resolveRoleAssignees(req.businessType(), starter.id(), flowVars);

        String businessKey = req.businessType() + ":" + req.businessId();
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                ver.processDefinitionKey(),
                businessKey,
                flowVars
        );

        LocalDateTime now = LocalDateTime.now();
        Map<String, String> snap = loadStarterSnapshots(starter.id());

        WfProcessInstance entity = new WfProcessInstance();
        entity.setId(nextWfId("wf_process_instance"));
        entity.setProcessInstanceId(pi.getId());
        entity.setTemplateId(ver.templateId());
        entity.setProcessVersionId(ver.versionId());
        entity.setBusinessType(req.businessType());
        entity.setBusinessId(req.businessId());
        entity.setTitle(req.title());
        entity.setStarterId(starter.id());
        entity.setStarterNameSnapshot(snap.get("realName"));
        entity.setStarterDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty() ? null : Long.parseLong(snap.get("deptId")));
        entity.setStarterDeptNameSnapshot(snap.get("deptName"));
        entity.setCurrentNodeName(firstRuntimeTaskName(pi.getId()));
        entity.setStatus(APPROVING);
        entity.setStartedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        instanceMapper.insert(entity);

        insertTaskRecord(entity.getId(), null, "SUBMIT", starter.id(), starter.realName(), "发起", null, null, now);
        syncPendingTasksFromFlowable(entity.getId(), pi.getId());

        auditService.safeRecordOperation(starter.id(), "WF_START", req.businessType(), entity.getId(), AuditService.SUCCESS, null);
        return instanceSummary(entity.getId());
    }

    // ─── Task lists ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> todoTasks(long page, long size) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        long total = wfTaskMapper.countTodoTasks(user.id(), PENDING);
        List<Map<String, Object>> items = wfTaskMapper.selectTodoTasks(user.id(), PENDING, ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> doneTasks(long page, long size) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        long total = wfTaskMapper.countDoneTasks(user.id());
        List<Map<String, Object>> items = wfTaskMapper.selectDoneTasks(user.id(), ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> startedByMe(long page, long size) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        long total = instanceMapper.selectCount(
                new LambdaQueryWrapper<WfProcessInstance>().eq(WfProcessInstance::getStarterId, user.id())
        );
        List<WfProcessInstance> items = instanceMapper.selectList(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getStarterId, user.id())
                        .orderByDesc(WfProcessInstance::getStartedAt)
                        .last("limit " + ps[1] + " offset " + (ps[0] - 1) * ps[1])
        );
        return new PageResponse<>(ps[0], ps[1], total, items.stream().map(this::toMap).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> ccToMe(long page, long size) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        Long total = ccRecordMapper.selectCount(
                new LambdaQueryWrapper<WfCcRecord>().eq(WfCcRecord::getReceiverId, user.id())
        );
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = ccRecordMapper.selectCcToMe(user.id(), ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], t, items);
    }

    // ─── CC operations ─────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> markCcRead(long ccRecordId) {
        AuthUser user = authService.currentUser();
        WfCcRecord cc = ccRecordMapper.selectById(ccRecordId);
        if (cc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "抄送记录不存在");
        }
        if (cc.getReceiverId() != user.id()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅接收人可标记已读");
        }
        LocalDateTime now = LocalDateTime.now();
        ccRecordMapper.markRead(ccRecordId, now);
        return Map.of("id", ccRecordId, "readAt", now.toString());
    }

    @Transactional
    public Map<String, Object> addCc(WorkflowDtos.CcAddRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> inst = loadInstance(req.wfInstanceId());
        long instanceId = ((Number) inst.get("wfInstanceId")).longValue();
        boolean superAdmin = user.permissions().contains("*");
        long starterId = ((Number) inst.get("starterId")).longValue();
        if (!superAdmin && !user.id().equals(starterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅发起人或超级管理员可抄送");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> created = new ArrayList<>();
        for (Long rid : req.receiverIds()) {
            if (rid == null) continue;
            Long exists = userMapper.selectCount(
                    new LambdaQueryWrapper<com.company.oa.entity.org.User>()
                            .eq(com.company.oa.entity.org.User::getId, rid)
                            .eq(com.company.oa.entity.org.User::getDeleted, 0)
            );
            if (exists == null || exists == 0) continue;
            WfCcRecord cc = new WfCcRecord();
            cc.setId(nextWfId("wf_cc_record"));
            cc.setWfInstanceId(instanceId);
            cc.setReceiverId(rid);
            cc.setCcReason(req.reason());
            cc.setCreatedBy(user.id());
            cc.setCreatedAt(now);
            ccRecordMapper.insert(cc);
            created.add(cc.getId());
        }
        return Map.of("wfInstanceId", instanceId, "created", created);
    }

    // ─── Instance detail & timeline ────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> instanceDetail(long wfInstanceId) {
        return loadInstance(wfInstanceId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> timeline(long wfInstanceId) {
        loadInstance(wfInstanceId);
        return taskRecordMapper.selectTimeline(wfInstanceId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> diagram(long wfInstanceId) {
        Map<String, Object> inst = loadInstance(wfInstanceId);
        String pid = String.valueOf(inst.get("processInstanceId"));
        List<String> active = new ArrayList<>();
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(pid).list();
        for (Task t : tasks) {
            active.add(t.getTaskDefinitionKey());
        }
        List<Map<String, Object>> history = new ArrayList<>();
        List<String> completedActivityIds = new ArrayList<>();
        try {
            List<HistoricActivityInstance> his = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(pid)
                    .orderByHistoricActivityInstanceStartTime().asc()
                    .list();
            for (HistoricActivityInstance h : his) {
                Map<String, Object> hm = new LinkedHashMap<>();
                hm.put("activityId", h.getActivityId());
                hm.put("activityName", h.getActivityName());
                hm.put("activityType", h.getActivityType());
                hm.put("startTime", h.getStartTime() == null ? null : h.getStartTime().toInstant().toString());
                hm.put("endTime", h.getEndTime() == null ? null : h.getEndTime().toInstant().toString());
                hm.put("assignee", h.getAssignee());
                hm.put("durationMs", h.getDurationInMillis());
                history.add(hm);
                if (h.getEndTime() != null && h.getActivityId() != null
                        && !"sequenceFlow".equalsIgnoreCase(h.getActivityType())) {
                    completedActivityIds.add(h.getActivityId());
                }
            }
        } catch (Exception ignore) {
            // 历史服务不可用时降级为空
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("wfInstanceId", wfInstanceId);
        out.put("processInstanceId", pid);
        out.put("instanceStatus", inst.get("status"));
        out.put("activeActivityIds", active);
        out.put("completedActivityIds", completedActivityIds);
        out.put("history", history);
        return out;
    }

    // ─── Approve / Reject / Transfer / Remind ──────────────────────────────

    @Transactional
    public Map<String, Object> approveTask(long wfTaskId, WorkflowDtos.ApproveRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> row = loadWfTask(wfTaskId);
        if (!PENDING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已处理");
        }
        long assigneeId = ((Number) row.get("assigneeId")).longValue();
        if (assigneeId != user.id()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有任务处理人可通过审批");
        }
        String flowableTaskId = String.valueOf(row.get("flowableTaskId"));
        String processInstanceId = String.valueOf(row.get("processInstanceId"));
        long wfInstanceId = ((Number) row.get("wfInstanceId")).longValue();
        Object originRaw = row.get("addSignOriginTaskId");
        Long originTaskId = originRaw == null ? null : ((Number) originRaw).longValue();
        String addSignMode = row.get("addSignMode") == null ? null : String.valueOf(row.get("addSignMode"));

        LocalDateTime now = LocalDateTime.now();
        String comment = req == null || req.comment() == null ? null : req.comment();
        String attach = req != null && req.attachmentIds() != null && !req.attachmentIds().isEmpty()
                ? req.attachmentIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                : null;

        // 顺序加签的回签：加签人通过后，把 Flowable 任务转回原审批人，原 wf_task 重新置为 PENDING。
        if (originTaskId != null && ADD_SIGN_SEQUENTIAL.equals(addSignMode)) {
            Map<String, Object> origin = loadWfTask(originTaskId);
            Long origAssigneeId = ((Number) origin.get("assigneeId")).longValue();
            taskService.setAssignee(flowableTaskId, String.valueOf(origAssigneeId));
            wfTaskMapper.updateStatusById(wfTaskId, COMPLETED, now);
            wfTaskMapper.updateStatusByIdAndOldStatus(originTaskId, PENDING, "ADD_SIGN_HOLDING");
            insertTaskRecord(wfInstanceId, wfTaskId, "APPROVE", user.id(), user.realName(),
                    String.valueOf(row.get("nodeName")),
                    (comment == null ? "" : comment) + " [前加签通过，回签到原审批人]", attach, now);
            auditService.safeRecordOperation(user.id(), "WF_APPROVE", "WF_TASK", wfTaskId, AuditService.SUCCESS, null);
            return instanceSummary(wfInstanceId);
        }

        // 并行加签场景：取消其它共享 flowable_task_id 的兄弟 PENDING 任务
        if (ADD_SIGN_PARALLEL.equals(addSignMode) || originTaskId == null) {
            wfTaskMapper.cancelParallelSiblings(flowableTaskId, wfTaskId, now);
        }

        taskService.complete(flowableTaskId);
        wfTaskMapper.updateStatusById(wfTaskId, COMPLETED, now);
        insertTaskRecord(wfInstanceId, wfTaskId, "APPROVE", user.id(), user.realName(),
                String.valueOf(row.get("nodeName")), comment, attach, now);

        if (isProcessEnded(processInstanceId)) {
            instanceMapper.updateStatus(wfInstanceId, APPROVED, null, now);
            syncOaDocumentFromWorkflow(wfInstanceId, APPROVED);
            // Notify starter: workflow fully approved
            Map<String, Object> inst = loadInstance(wfInstanceId);
            long starterId = ((Number) inst.get("starterId")).longValue();
            String title = String.valueOf(inst.get("title"));
            messageService.send(starterId, "WORKFLOW", "您的申请已审批通过: " + title,
                    "您的" + title + "已通过全部审批节点，请查看审批结果。", "WORKFLOW", null, wfInstanceId);
        } else {
            instanceMapper.updateCurrentNode(wfInstanceId, firstRuntimeTaskName(processInstanceId));
            syncPendingTasksFromFlowable(wfInstanceId, processInstanceId);
            // Notify next assignees: new approval task
            Map<String, Object> inst = loadInstance(wfInstanceId);
            String title = String.valueOf(inst.get("title"));
            List<Long> pendingAssigneeIds = wfTaskMapper.selectAssigneeIdsByInstanceAndStatus(wfInstanceId, PENDING);
            for (Long nextAssigneeId : pendingAssigneeIds) {
                messageService.send(nextAssigneeId, "WORKFLOW", "您有新的审批任务: " + title,
                        "请及时处理" + title + "的审批。", "WORKFLOW", null, wfInstanceId);
            }
        }
        auditService.safeRecordOperation(user.id(), "WF_APPROVE", "WF_TASK", wfTaskId, AuditService.SUCCESS, null);
        return instanceSummary(wfInstanceId);
    }

    @Transactional
    public Map<String, Object> rejectTask(long wfTaskId, WorkflowDtos.RejectRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> row = loadWfTask(wfTaskId);
        if (!PENDING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已处理");
        }
        if (((Number) row.get("assigneeId")).longValue() != user.id()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有任务处理人可驳回");
        }
        String processInstanceId = String.valueOf(row.get("processInstanceId"));
        long wfInstanceId = ((Number) row.get("wfInstanceId")).longValue();
        String comment = req == null ? null : req.comment();

        runtimeService.deleteProcessInstance(processInstanceId, "REJECT:" + (comment == null ? "" : comment));

        LocalDateTime now = LocalDateTime.now();
        wfTaskMapper.updateStatusByInstanceAndOldStatus(wfInstanceId, CANCELLED, now, PENDING);
        instanceMapper.updateStatus(wfInstanceId, REJECTED, null, now);
        syncOaDocumentFromWorkflow(wfInstanceId, REJECTED);
        insertTaskRecord(wfInstanceId, wfTaskId, "REJECT", user.id(), user.realName(),
                String.valueOf(row.get("nodeName")), comment, null, now);

        auditService.safeRecordOperation(user.id(), "WF_REJECT", "WF_TASK", wfTaskId, AuditService.SUCCESS, null);
        // Notify starter: workflow rejected
        Map<String, Object> inst = loadInstance(wfInstanceId);
        long starterId = ((Number) inst.get("starterId")).longValue();
        String title = String.valueOf(inst.get("title"));
        messageService.send(starterId, "WORKFLOW", "您的申请已被驳回: " + title,
                "您的" + title + "审批未通过，原因: " + (comment == null ? "无" : comment) + "。请修改后重新提交。", "WORKFLOW", null, wfInstanceId);
        return instanceSummary(wfInstanceId);
    }

    @Transactional
    public Map<String, Object> transferTask(long wfTaskId, WorkflowDtos.TransferRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> row = loadWfTask(wfTaskId);
        if (!PENDING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已处理");
        }
        if (((Number) row.get("assigneeId")).longValue() != user.id()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有任务处理人可转交");
        }
        Long targetUserId = req.targetUserId();
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<com.company.oa.entity.org.User>()
                        .eq(com.company.oa.entity.org.User::getId, targetUserId)
                        .eq(com.company.oa.entity.org.User::getDeleted, 0)
        );
        if (exists == null || exists == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "转交目标用户不存在");
        }
        String flowableTaskId = String.valueOf(row.get("flowableTaskId"));
        Map<String, String> target = loadUserSnapshot(targetUserId);
        taskService.setAssignee(flowableTaskId, String.valueOf(targetUserId));

        Long deptId = target.get("deptId") == null || target.get("deptId").isEmpty() ? null : Long.parseLong(target.get("deptId"));
        wfTaskMapper.updateAssignee(wfTaskId, targetUserId, target.get("realName"), deptId);

        LocalDateTime now = LocalDateTime.now();
        long wfInstanceId = ((Number) row.get("wfInstanceId")).longValue();
        insertTaskRecord(wfInstanceId, wfTaskId, "TRANSFER", user.id(), user.realName(),
                String.valueOf(row.get("nodeName")), req.comment(), null, now);

        auditService.safeRecordOperation(user.id(), "WF_TRANSFER", "WF_TASK", wfTaskId, AuditService.SUCCESS, null);
        return Map.of("taskId", wfTaskId, "transferredTo", targetUserId, "operatedAt", now.toString());
    }

    public Map<String, Object> remindTask(long wfTaskId) {
        loadWfTask(wfTaskId);
        return Map.of("taskId", wfTaskId, "success", true);
    }

    // ─── Add Sign ──────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> addSign(long wfTaskId, WorkflowDtos.AddSignRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> row = loadWfTask(wfTaskId);
        if (!PENDING.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已处理，无法加签");
        }
        if (((Number) row.get("assigneeId")).longValue() != user.id()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅当前任务处理人可加签");
        }
        if (req.assigneeUserId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "加签人必填");
        }
        if (req.assigneeUserId() == user.id()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "加签人不能为自己");
        }
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<com.company.oa.entity.org.User>()
                        .eq(com.company.oa.entity.org.User::getId, req.assigneeUserId())
                        .eq(com.company.oa.entity.org.User::getDeleted, 0)
        );
        if (exists == null || exists == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "加签人不存在");
        }
        String mode = req.mode() == null ? ADD_SIGN_SEQUENTIAL : req.mode().toUpperCase();
        if (!ADD_SIGN_SEQUENTIAL.equals(mode) && !ADD_SIGN_PARALLEL.equals(mode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "加签模式仅支持 SEQUENTIAL/PARALLEL");
        }
        long wfInstanceId = ((Number) row.get("wfInstanceId")).longValue();
        String processInstanceId = String.valueOf(row.get("processInstanceId"));
        String flowableTaskId = String.valueOf(row.get("flowableTaskId"));
        Map<String, String> target = loadUserSnapshot(req.assigneeUserId());
        Long deptId = target.get("deptId") == null || target.get("deptId").isEmpty() ? null : Long.parseLong(target.get("deptId"));
        LocalDateTime now = LocalDateTime.now();

        if (ADD_SIGN_SEQUENTIAL.equals(mode)) {
            taskService.setAssignee(flowableTaskId, String.valueOf(req.assigneeUserId()));
            wfTaskMapper.updateStatusByIdAndOldStatus(wfTaskId, "ADD_SIGN_HOLDING", null);
        }

        WfTask newTask = new WfTask();
        newTask.setId(nextWfId("wf_task"));
        newTask.setFlowableTaskId(flowableTaskId);
        newTask.setProcessInstanceId(processInstanceId);
        newTask.setWfInstanceId(wfInstanceId);
        newTask.setNodeId(row.get("nodeId") == null ? "" : String.valueOf(row.get("nodeId")));
        newTask.setNodeName(String.valueOf(row.get("nodeName")));
        newTask.setAssigneeId(req.assigneeUserId());
        newTask.setAssigneeNameSnapshot(target.get("realName"));
        newTask.setAssigneeDeptId(deptId);
        newTask.setTaskType(TASK_TYPE_APPROVE);
        newTask.setStatus(PENDING);
        newTask.setCreatedAt(now);
        newTask.setAddSignOriginTaskId(wfTaskId);
        newTask.setAddSignMode(mode);
        wfTaskMapper.insert(newTask);

        insertTaskRecord(wfInstanceId, wfTaskId, "ADD_SIGN", user.id(), user.realName(),
                String.valueOf(row.get("nodeName")), req.comment(), null, now);
        auditService.safeRecordOperation(user.id(), "WF_ADD_SIGN", "WF_TASK", wfTaskId, AuditService.SUCCESS, null);
        return Map.of("taskId", newTask.getId(), "mode", mode, "operatedAt", now.toString());
    }

    // ─── Delegation ────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> createDelegation(WorkflowDtos.DelegateCreateRequest req) {
        AuthUser user = authService.currentUser();
        if (req.delegateeId() == null || req.delegateeId() == user.id()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "委托人不能为自己");
        }
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<com.company.oa.entity.org.User>()
                        .eq(com.company.oa.entity.org.User::getId, req.delegateeId())
                        .eq(com.company.oa.entity.org.User::getDeleted, 0)
        );
        if (exists == null || exists == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "受托人不存在");
        }
        java.time.OffsetDateTime startAt = req.startAt();
        java.time.OffsetDateTime endAt = req.endAt();
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "委托时间区间不合法");
        }
        LocalDateTime now = LocalDateTime.now();
        WfDelegation entity = new WfDelegation();
        entity.setId(nextWfId("wf_delegation"));
        entity.setDelegatorId(user.id());
        entity.setDelegateeId(req.delegateeId());
        entity.setBusinessScope(req.businessScope());
        entity.setStartAt(startAt.toLocalDateTime());
        entity.setEndAt(endAt.toLocalDateTime());
        entity.setStatus(DELEGATION_ACTIVE);
        entity.setReason(req.reason());
        entity.setCreatedAt(now);
        delegationMapper.insert(entity);
        auditService.safeRecordOperation(user.id(), "WF_DELEGATE_CREATE", "WF_DELEGATION", entity.getId(), AuditService.SUCCESS, null);
        return Map.of("id", entity.getId(), "status", DELEGATION_ACTIVE, "createdAt", now.toString());
    }

    @Transactional
    public Map<String, Object> cancelDelegation(long id) {
        AuthUser user = authService.currentUser();
        WfDelegation d = delegationMapper.selectById(id);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "委托不存在");
        }
        boolean superAdmin = user.permissions().contains("*");
        if (!superAdmin && !user.id().equals(d.getDelegatorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅委托人或超级管理员可取消委托");
        }
        if (!DELEGATION_ACTIVE.equals(d.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅生效中的委托可取消");
        }
        LocalDateTime now = LocalDateTime.now();
        WfDelegation upd = new WfDelegation();
        upd.setId(id);
        upd.setStatus(DELEGATION_CANCELLED);
        upd.setCancelledAt(now);
        int n = delegationMapper.update(upd,
                new LambdaQueryWrapper<WfDelegation>()
                        .eq(WfDelegation::getId, id)
                        .eq(WfDelegation::getStatus, DELEGATION_ACTIVE)
        );
        if (n == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "委托状态已变更");
        }
        auditService.safeRecordOperation(user.id(), "WF_DELEGATE_CANCEL", "WF_DELEGATION", id, AuditService.SUCCESS, null);
        return Map.of("id", id, "status", DELEGATION_CANCELLED);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listMyDelegations(long page, long size) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        Long total = delegationMapper.countMyDelegations(user.id());
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = delegationMapper.selectMyDelegations(user.id(), ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], t, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listAllDelegations(long page, long size, String status) {
        long[] ps = paginationHelper.clamp(page, size);
        Long total = delegationMapper.countAllDelegations(status);
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = delegationMapper.selectAllDelegations(status, ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], t, items);
    }

    // ─── Exceptions ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listExceptions(long page, long size) {
        long[] ps = paginationHelper.clamp(page, size);
        Long total = instanceMapper.countExceptions();
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = instanceMapper.selectExceptions(ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], t, items);
    }

    // ─── Withdraw / Terminate ──────────────────────────────────────────────

    @Transactional
    public Map<String, Object> withdrawInstance(long wfInstanceId) {
        AuthUser user = authService.currentUser();
        Map<String, Object> inst = loadInstance(wfInstanceId);
        if (!APPROVING.equals(String.valueOf(inst.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审批中的流程可撤回");
        }
        long starterId = ((Number) inst.get("starterId")).longValue();
        if (!user.id().equals(starterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅发起人可撤回");
        }
        Long approved = taskRecordMapper.countByInstanceIdAndAction(wfInstanceId, "APPROVE");
        if (approved != null && approved > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已有审批通过记录，不能撤回");
        }
        String pid = String.valueOf(inst.get("processInstanceId"));
        runtimeService.deleteProcessInstance(pid, "WITHDRAW");

        LocalDateTime now = LocalDateTime.now();
        wfTaskMapper.updateStatusByInstanceAndOldStatus(wfInstanceId, CANCELLED, now, PENDING);
        instanceMapper.updateStatus(wfInstanceId, WITHDRAWN, null, now);
        syncOaDocumentFromWorkflow(wfInstanceId, WITHDRAWN);
        insertTaskRecord(wfInstanceId, null, "WITHDRAW", user.id(), user.realName(), "撤回", null, null, now);
        auditService.safeRecordOperation(user.id(), "WF_WITHDRAW", "WF_INSTANCE", wfInstanceId, AuditService.SUCCESS, null);
        return instanceSummary(wfInstanceId);
    }

    @Transactional
    public Map<String, Object> terminateInstance(long wfInstanceId) {
        AuthUser user = authService.currentUser();
        Map<String, Object> inst = loadInstance(wfInstanceId);
        if (!APPROVING.equals(String.valueOf(inst.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅审批中的流程可终止");
        }
        long starterId = ((Number) inst.get("starterId")).longValue();
        boolean superAdmin = user.permissions().contains("*");
        if (!superAdmin && !user.id().equals(starterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅发起人或超级管理员可终止流程");
        }
        String pid = String.valueOf(inst.get("processInstanceId"));
        runtimeService.deleteProcessInstance(pid, "TERMINATE");

        LocalDateTime now = LocalDateTime.now();
        wfTaskMapper.updateStatusByInstanceAndOldStatus(wfInstanceId, CANCELLED, now, PENDING);
        instanceMapper.updateStatus(wfInstanceId, TERMINATED, null, now);
        syncOaDocumentFromWorkflow(wfInstanceId, TERMINATED);
        insertTaskRecord(wfInstanceId, null, "TERMINATE", user.id(), user.realName(), "终止", null, null, now);
        auditService.safeRecordOperation(user.id(), "WF_TERMINATE", "WF_INSTANCE", wfInstanceId, AuditService.SUCCESS, null);
        // Notify starter: workflow terminated
        messageService.send(starterId, "WORKFLOW", "您的申请已被终止: " + inst.get("title"),
                "您的" + inst.get("title") + "流程已被管理员终止。", "WORKFLOW", null, wfInstanceId);
        return instanceSummary(wfInstanceId);
    }

    // ─── Sync OA document status ───────────────────────────────────────────

    /**
     * 流程终态时回写 OA 业务单（请假、报销、用章、采购等），与 {@code WORKFLOW_RULES} 状态一致。
     */
    private void syncOaDocumentFromWorkflow(long wfInstanceId, String wfTerminalStatus) {
        Map<String, Object> inst = loadInstance(wfInstanceId);
        String businessType = String.valueOf(inst.get("businessType"));
        Object bidObj = inst.get("businessId");
        if (bidObj == null) {
            return;
        }
        long businessId = ((Number) bidObj).longValue();
        String docStatus;
        boolean clearFlowKeys;
        switch (wfTerminalStatus) {
            case APPROVED -> {
                docStatus = APPROVED;
                clearFlowKeys = false;
            }
            case REJECTED -> {
                docStatus = REJECTED;
                clearFlowKeys = true;
            }
            case WITHDRAWN -> {
                docStatus = WITHDRAWN;
                clearFlowKeys = true;
            }
            case TERMINATED -> {
                docStatus = CANCELLED;
                clearFlowKeys = true;
            }
            default -> {
                return;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        switch (businessType) {
            case "LEAVE" -> {
                if (clearFlowKeys) leaveMapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else leaveMapper.updateStatusById(businessId, docStatus, now);
            }
            case "EXPENSE" -> {
                if (clearFlowKeys) expenseMapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else expenseMapper.updateStatusById(businessId, docStatus, now);
            }
            case "SEAL" -> {
                if (clearFlowKeys) sealApplyMapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else sealApplyMapper.updateStatusById(businessId, docStatus, now);
            }
            case "PURCHASE" -> {
                if (clearFlowKeys) purchaseMapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else purchaseMapper.updateStatusById(businessId, docStatus, now);
            }
            case "CONTRACT" -> {
                if (clearFlowKeys) contractMapper.updateStatusClearFlowKeysById(businessId, docStatus, now);
                else contractMapper.updateStatusById(businessId, docStatus, now);
            }
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private Map<String, Object> loadWfTask(long wfTaskId) {
        Map<String, Object> row = wfTaskMapper.loadWfTask(wfTaskId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return new LinkedHashMap<>(row);
    }

    private Map<String, Object> loadInstance(long wfInstanceId) {
        Map<String, Object> row = instanceMapper.loadInstance(wfInstanceId);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程实例不存在");
        }
        return new LinkedHashMap<>(row);
    }

    private Map<String, Object> instanceSummary(long wfInstanceId) {
        Map<String, Object> inst = loadInstance(wfInstanceId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("wfInstanceId", inst.get("wfInstanceId"));
        out.put("processInstanceId", inst.get("processInstanceId"));
        out.put("status", inst.get("status"));
        out.put("currentNodeName", inst.get("currentNodeName"));
        return out;
    }

    private void assertNoActiveInstance(String businessType, long businessId) {
        Long n = instanceMapper.selectCount(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getBusinessType, businessType)
                        .eq(WfProcessInstance::getBusinessId, businessId)
                        .eq(WfProcessInstance::getStatus, APPROVING)
        );
        if (n != null && n > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该业务已存在审批中的流程实例");
        }
    }

    private PublishedVersion resolvePublishedVersion(String businessType) {
        Map<String, Object> r = templateMapper.resolvePublishedVersion(businessType);
        if (r == null && !"GENERIC".equals(businessType)) {
            r = templateMapper.resolveGenericPublishedVersion();
        }
        if (r == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到已发布的流程版本");
        }
        return new PublishedVersion(
                ((Number) r.get("templateId")).longValue(),
                ((Number) r.get("versionId")).longValue(),
                String.valueOf(r.get("procKey"))
        );
    }

    private void resolveRoleAssignees(String businessType, long starterId, Map<String, Object> flowVars) {
        // 部门主管：从发起人的 org_user.manager_user_id 获取
        if (!flowVars.containsKey("deptHeadId")) {
            Long mgrId = userMapper.selectManagerUserId(starterId);
            if (mgrId != null) {
                flowVars.put("deptHeadId", String.valueOf(mgrId));
            }
        }
        // 总经理：角色为 SUPER_ADMIN 的第一个用户（或由调用方指定）
        if (!flowVars.containsKey("gmId")) {
            Long gmId = userRoleMapper.findFirstUserIdByRoleCode("SUPER_ADMIN");
            if (gmId != null) {
                flowVars.put("gmId", String.valueOf(gmId));
            }
        }
        // 按业务类型解析特定角色
        switch (businessType) {
            case "LEAVE" -> {
                if (!flowVars.containsKey("hrAdminId")) {
                    Long hrId = userRoleMapper.findFirstUserIdByRoleCode("HR_ADMIN");
                    if (hrId != null) flowVars.put("hrAdminId", String.valueOf(hrId));
                }
                if (!flowVars.containsKey("vpId")) {
                    Long vpId = userRoleMapper.findFirstUserIdByRoleCode("LEADER");
                    if (vpId != null) flowVars.put("vpId", String.valueOf(vpId));
                }
            }
            case "EXPENSE" -> {
                if (!flowVars.containsKey("financeAdminId")) {
                    Long faId = userRoleMapper.findFirstUserIdByRoleCode("FINANCE_ADMIN");
                    if (faId != null) flowVars.put("financeAdminId", String.valueOf(faId));
                }
            }
            case "SEAL", "PURCHASE" -> {
                if (!flowVars.containsKey("adminOfficerId")) {
                    Long aoId = userRoleMapper.findFirstUserIdByRoleCode("ADMIN_OFFICER");
                    if (aoId != null) flowVars.put("adminOfficerId", String.valueOf(aoId));
                }
            }
            case "CONTRACT" -> {
                if (!flowVars.containsKey("contractAdminId")) {
                    Long caId = userRoleMapper.findFirstUserIdByRoleCode("CONTRACT_ADMIN");
                    if (caId != null) flowVars.put("contractAdminId", String.valueOf(caId));
                }
            }
        }
        // Fallback: any unresolved role assignee defaults to the starter
        for (String key : List.of("deptHeadId", "gmId", "hrAdminId", "vpId", "financeAdminId", "adminOfficerId", "contractAdminId")) {
            if (!flowVars.containsKey(key)) {
                flowVars.put(key, String.valueOf(starterId));
            }
        }
    }

    private Long resolveManagerId(long starterId, Map<String, Object> variables) {
        if (variables != null && variables.get("managerId") != null) {
            Object v = variables.get("managerId");
            if (v instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(String.valueOf(v));
        }
        Long mId = userMapper.selectManagerUserId(starterId);
        if (mId != null) {
            return mId;
        }
        // No manager configured — fall back to self (top-level user)
        return starterId;
    }

    private Map<String, String> loadStarterSnapshots(long userId) {
        Map<String, Object> r = userMapper.selectUserSnapshot(userId);
        if (r == null || r.isEmpty()) {
            return Map.of("realName", "", "deptId", "", "deptName", "");
        }
        Map<String, String> m = new LinkedHashMap<>();
        m.put("realName", r.get("realName") == null ? "" : String.valueOf(r.get("realName")));
        m.put("deptName", r.get("deptName") == null ? "" : String.valueOf(r.get("deptName")));
        if (r.get("deptId") != null) {
            m.put("deptId", String.valueOf(((Number) r.get("deptId")).longValue()));
        } else {
            m.put("deptId", "");
        }
        return m;
    }

    private Map<String, String> loadUserSnapshot(long userId) {
        return loadStarterSnapshots(userId);
    }

    private String firstRuntimeTaskName(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (tasks.isEmpty()) {
            return null;
        }
        return tasks.get(0).getName();
    }

    private boolean isProcessEnded(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count() == 0;
    }

    private void syncPendingTasksFromFlowable(long wfInstanceId, String processInstanceId) {
        wfTaskMapper.deletePlainPendingByInstance(wfInstanceId, PENDING);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        LocalDateTime now = LocalDateTime.now();
        for (Task t : tasks) {
            long assigneeId = Long.parseLong(t.getAssignee());
            // 委托：若 assignee 在生效委托期内，则把任务直接路由到受托人
            Long delegated = delegationMapper.findActiveDelegateeFor(assigneeId, now);
            if (delegated != null) {
                taskService.setAssignee(t.getId(), String.valueOf(delegated));
                assigneeId = delegated;
            }
            Map<String, String> snap = loadUserSnapshot(assigneeId);
            WfTask entity = new WfTask();
            entity.setId(nextWfId("wf_task"));
            entity.setFlowableTaskId(t.getId());
            entity.setProcessInstanceId(processInstanceId);
            entity.setWfInstanceId(wfInstanceId);
            entity.setNodeId(t.getTaskDefinitionKey());
            entity.setNodeName(t.getName());
            entity.setAssigneeId(assigneeId);
            entity.setAssigneeNameSnapshot(snap.get("realName"));
            entity.setAssigneeDeptId(snap.get("deptId") == null || snap.get("deptId").isEmpty() ? null : Long.parseLong(snap.get("deptId")));
            entity.setTaskType(TASK_TYPE_APPROVE);
            entity.setStatus(PENDING);
            entity.setCreatedAt(now);
            wfTaskMapper.insert(entity);
        }
    }

    private void insertTaskRecord(
            long wfInstanceId,
            Long wfTaskId,
            String action,
            long operatorId,
            String operatorName,
            String nodeName,
            String comment,
            String attachmentIds,
            LocalDateTime operatedAt
    ) {
        WfTaskRecord entity = new WfTaskRecord();
        entity.setId(nextWfId("wf_task_record"));
        entity.setWfInstanceId(wfInstanceId);
        entity.setTaskId(wfTaskId);
        entity.setAction(action);
        entity.setOperatorId(operatorId);
        entity.setOperatorNameSnapshot(operatorName);
        entity.setNodeName(nodeName);
        entity.setComment(comment);
        entity.setAttachmentIds(attachmentIds);
        entity.setOperatedAt(operatedAt);
        taskRecordMapper.insert(entity);
    }

    private long nextWfId(String table) {
        return sequenceService.nextId(table);
    }

}
