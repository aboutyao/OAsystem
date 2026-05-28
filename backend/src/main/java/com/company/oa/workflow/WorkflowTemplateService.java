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
import com.company.oa.entity.wf.WfProcessTemplate;
import com.company.oa.entity.wf.WfProcessVersion;
import com.company.oa.workflow.mapper.WfProcessTemplateMapper;
import com.company.oa.workflow.mapper.WfProcessVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flowable.engine.RepositoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowTemplateService {

    private final WfProcessTemplateMapper templateMapper;
    private final WfProcessVersionMapper versionMapper;
    private final RepositoryService repositoryService;
    private final AuthService authService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final PaginationHelper paginationHelper;

    public WorkflowTemplateService(
            WfProcessTemplateMapper templateMapper,
            WfProcessVersionMapper versionMapper,
            RepositoryService repositoryService,
            AuthService authService,
            AuditService auditService,
            SequenceService sequenceService,
            PaginationHelper paginationHelper
    ) {
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.repositoryService = repositoryService;
        this.authService = authService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.paginationHelper = paginationHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listTemplates(long page, long size) {
        long[] ps = paginationHelper.clamp(page, size);
        long total = templateMapper.selectCount(new LambdaQueryWrapper<WfProcessTemplate>());
        List<WfProcessTemplate> items = templateMapper.selectList(
                new LambdaQueryWrapper<WfProcessTemplate>()
                        .orderByAsc(WfProcessTemplate::getId)
                        .last("limit " + ps[1] + " offset " + (ps[0] - 1) * ps[1])
        );
        return new PageResponse<>(ps[0], ps[1], total, items.stream().map(OaEntityMapper::toMap).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> templateDetail(long id) {
        WfProcessTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流程模板不存在");
        }
        Map<String, Object> m = OaEntityMapper.toMap(t);
        List<WfProcessVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<WfProcessVersion>()
                        .eq(WfProcessVersion::getTemplateId, id)
                        .orderByDesc(WfProcessVersion::getVersionNo)
        );
        m.put("versions", versions.stream().map(OaEntityMapper::toMap).collect(Collectors.toList()));
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
        entity.setId(sequenceService.nextId("wf_process_template"));
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
        entity.setId(sequenceService.nextId("wf_process_version"));
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

        versionMapper.archivePublished(templateId, now);

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
}
