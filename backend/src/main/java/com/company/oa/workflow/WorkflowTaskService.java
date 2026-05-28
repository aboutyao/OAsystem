package com.company.oa.workflow;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.OaEntityMapper;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.wf.WfCcRecord;
import com.company.oa.entity.wf.WfTask;
import com.company.oa.entity.wf.WfTaskRecord;
import com.company.oa.message.EmailService;
import com.company.oa.message.MessageService;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.mapper.WfCcRecordMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import com.company.oa.workflow.mapper.WfTaskRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowTaskService {
    private static final String PENDING = "PENDING";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";
    private static final String ADD_SIGN_SEQUENTIAL = "SEQUENTIAL";
    private static final String ADD_SIGN_PARALLEL = "PARALLEL";
    private static final String TASK_TYPE_APPROVE = "APPROVE";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final AuthService authService;
    private final AuditService auditService;
    private final MessageService messageService;
    private final EmailService emailService;
    private final SequenceService sequenceService;
    private final WfTaskMapper wfTaskMapper;
    private final WfTaskRecordMapper taskRecordMapper;
    private final WfCcRecordMapper ccRecordMapper;
    private final WfProcessInstanceMapper instanceMapper;
    private final UserMapper userMapper;
    private final PaginationHelper paginationHelper;

    public WorkflowTaskService(
            RuntimeService runtimeService,
            TaskService taskService,
            AuthService authService,
            AuditService auditService,
            MessageService messageService,
            EmailService emailService,
            SequenceService sequenceService,
            WfTaskMapper wfTaskMapper,
            WfTaskRecordMapper taskRecordMapper,
            WfCcRecordMapper ccRecordMapper,
            WfProcessInstanceMapper instanceMapper,
            UserMapper userMapper,
            PaginationHelper paginationHelper
    ) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.authService = authService;
        this.auditService = auditService;
        this.messageService = messageService;
        this.emailService = emailService;
        this.sequenceService = sequenceService;
        this.wfTaskMapper = wfTaskMapper;
        this.taskRecordMapper = taskRecordMapper;
        this.ccRecordMapper = ccRecordMapper;
        this.instanceMapper = instanceMapper;
        this.userMapper = userMapper;
        this.paginationHelper = paginationHelper;
    }

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

        // Sequential add-sign: after add-signer approves, transfer back to original approver
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

        // Parallel add-sign: cancel sibling pending tasks sharing the same flowable task
        if (ADD_SIGN_PARALLEL.equals(addSignMode) || originTaskId == null) {
            wfTaskMapper.cancelParallelSiblings(flowableTaskId, wfTaskId, now);
        }

        taskService.complete(flowableTaskId);
        wfTaskMapper.updateStatusById(wfTaskId, COMPLETED, now);
        insertTaskRecord(wfInstanceId, wfTaskId, "APPROVE", user.id(), user.realName(),
                String.valueOf(row.get("nodeName")), comment, attach, now);

        boolean processEnded = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() == 0;

        if (processEnded) {
            instanceMapper.updateStatus(wfInstanceId, "APPROVED", null, now);
            WorkflowDocumentSyncer.sync(wfInstanceId, "APPROVED", instanceMapper);
            Map<String, Object> inst = loadInstance(wfInstanceId);
            long starterId = ((Number) inst.get("starterId")).longValue();
            String title = String.valueOf(inst.get("title"));
            messageService.send(starterId, "WORKFLOW", "您的申请已审批通过: " + title,
                    "您的" + title + "已通过全部审批节点，请查看审批结果。", "WORKFLOW", null, wfInstanceId);
            String email = getUserEmail(starterId);
            if (email != null) {
                emailService.sendApprovalNotification(email, "您的申请已审批通过: " + title,
                        "您的" + title + "已通过全部审批节点，请查看审批结果。");
            }
        } else {
            instanceMapper.updateCurrentNode(wfInstanceId, firstRuntimeTaskName(processInstanceId));
            Map<String, Object> inst = loadInstance(wfInstanceId);
            String title = String.valueOf(inst.get("title"));
            List<Long> pendingAssigneeIds = wfTaskMapper.selectAssigneeIdsByInstanceAndStatus(wfInstanceId, PENDING);
            for (Long nextAssigneeId : pendingAssigneeIds) {
                messageService.send(nextAssigneeId, "WORKFLOW", "您有新的审批任务: " + title,
                        "请及时处理" + title + "的审批。", "WORKFLOW", null, wfInstanceId);
                String assigneeEmail = getUserEmail(nextAssigneeId);
                if (assigneeEmail != null) {
                    emailService.sendApprovalNotification(assigneeEmail, "您有新的审批任务: " + title,
                            "请及时处理" + title + "的审批。");
                }
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
        instanceMapper.updateStatus(wfInstanceId, "REJECTED", null, now);
        WorkflowDocumentSyncer.sync(wfInstanceId, "REJECTED", instanceMapper);
        insertTaskRecord(wfInstanceId, wfTaskId, "REJECT", user.id(), user.realName(),
                String.valueOf(row.get("nodeName")), comment, null, now);

        auditService.safeRecordOperation(user.id(), "WF_REJECT", "WF_TASK", wfTaskId, AuditService.SUCCESS, null);
        Map<String, Object> inst = loadInstance(wfInstanceId);
        long starterId = ((Number) inst.get("starterId")).longValue();
        String title = String.valueOf(inst.get("title"));
        messageService.send(starterId, "WORKFLOW", "您的申请已被驳回: " + title,
                "您的" + title + "审批未通过，原因: " + (comment == null ? "无" : comment) + "。请修改后重新提交。", "WORKFLOW", null, wfInstanceId);
        String rejectEmail = getUserEmail(starterId);
        if (rejectEmail != null) {
            emailService.sendApprovalNotification(rejectEmail, "您的申请已被驳回: " + title,
                    "您的" + title + "审批未通过，原因: " + (comment == null ? "无" : comment) + "。请修改后重新提交。");
        }
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

    @Transactional
    public Map<String, Object> remindTask(long wfTaskId) {
        AuthUser user = authService.currentUser();
        Map<String, Object> task = loadWfTask(wfTaskId);
        if (!PENDING.equals(String.valueOf(task.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已处理，无需催办");
        }
        long assigneeId = ((Number) task.get("assigneeId")).longValue();
        long wfInstanceId = ((Number) task.get("wfInstanceId")).longValue();
        Map<String, Object> inst = loadInstance(wfInstanceId);
        String title = String.valueOf(inst.get("title"));
        String nodeName = String.valueOf(task.getOrDefault("taskName", "审批节点"));
        messageService.send(assigneeId, "WORKFLOW", "催办提醒: " + title,
                user.realName() + " 催促您尽快处理「" + title + "」的「" + nodeName + "」节点。",
                "WORKFLOW", null, wfInstanceId);
        String remindEmail = getUserEmail(assigneeId);
        if (remindEmail != null) {
            emailService.sendApprovalNotification(remindEmail, "催办提醒: " + title,
                    user.realName() + " 催促您尽快处理「" + title + "」的「" + nodeName + "」节点。");
        }
        auditService.safeRecordOperation(user.id(), "WF_REMIND", "WF_TASK", wfTaskId, AuditService.SUCCESS, null);
        return Map.of("taskId", wfTaskId, "success", true, "remindedAt", LocalDateTime.now().toString());
    }

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
        newTask.setId(sequenceService.nextId("wf_task"));
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
            Long userExists = userMapper.selectCount(
                    new LambdaQueryWrapper<com.company.oa.entity.org.User>()
                            .eq(com.company.oa.entity.org.User::getId, rid)
                            .eq(com.company.oa.entity.org.User::getDeleted, 0)
            );
            if (userExists == null || userExists == 0) continue;
            WfCcRecord cc = new WfCcRecord();
            cc.setId(sequenceService.nextId("wf_cc_record"));
            cc.setWfInstanceId(instanceId);
            cc.setReceiverId(rid);
            cc.setCcReason(req.reason());
            cc.setCreatedBy(user.id());
            cc.setCreatedAt(now);
            ccRecordMapper.insert(cc);
            created.add(cc.getId());
            String ccEmail = getUserEmail(rid);
            if (ccEmail != null) {
                String instTitle = String.valueOf(inst.get("title"));
                emailService.sendApprovalNotification(ccEmail, "您有一条新的抄送通知: " + instTitle,
                        user.realName() + " 将「" + instTitle + "」抄送给您。" +
                                (req.reason() == null ? "" : " 原因: " + req.reason()));
            }
        }
        return Map.of("wfInstanceId", instanceId, "created", created);
    }

    @Transactional
    public Map<String, Object> batchApprove(WorkflowDtos.BatchApproveRequest request) {
        int success = 0;
        int failed = 0;
        for (Long taskId : request.taskIds()) {
            try {
                approveTask(taskId, new WorkflowDtos.ApproveRequest(request.comment(), null));
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("total", request.taskIds().size());
        return result;
    }

    // ─── Private helpers ───────────────────────────────────────────────

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
        out.put("slaDeadline", inst.get("slaDeadline"));
        out.put("slaBreached", inst.get("slaBreached"));
        return out;
    }

    private Map<String, String> loadUserSnapshot(long userId) {
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

    private String firstRuntimeTaskName(String processInstanceId) {
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (tasks.isEmpty()) {
            return null;
        }
        return tasks.get(0).getName();
    }

    private void insertTaskRecord(long wfInstanceId, Long wfTaskId, String action,
                                  long operatorId, String operatorName, String nodeName,
                                  String comment, String attachmentIds, LocalDateTime operatedAt) {
        WfTaskRecord entity = new WfTaskRecord();
        entity.setId(sequenceService.nextId("wf_task_record"));
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

    private String getUserEmail(long userId) {
        com.company.oa.entity.org.User user = userMapper.selectById(userId);
        return user != null ? user.getEmail() : null;
    }
}
