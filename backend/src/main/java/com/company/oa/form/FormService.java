package com.company.oa.form;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.form.FormFieldRule;
import com.company.oa.entity.form.FormSnapshot;
import com.company.oa.entity.form.FormTemplate;
import com.company.oa.entity.form.FormVersion;
import com.company.oa.form.mapper.FormFieldRuleMapper;
import com.company.oa.form.mapper.FormSnapshotMapper;
import com.company.oa.form.mapper.FormTemplateMapper;
import com.company.oa.form.mapper.FormVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FormService {

    private final FormTemplateMapper formTemplateMapper;
    private final FormVersionMapper formVersionMapper;
    private final FormFieldRuleMapper formFieldRuleMapper;
    private final FormSnapshotMapper formSnapshotMapper;
    private final AuthService auth;
    private final SequenceService sequenceService;
    private final ObjectMapper json = new ObjectMapper();

    public FormService(FormTemplateMapper formTemplateMapper, FormVersionMapper formVersionMapper,
                       FormFieldRuleMapper formFieldRuleMapper, FormSnapshotMapper formSnapshotMapper,
                       AuthService auth, SequenceService sequenceService) {
        this.formTemplateMapper = formTemplateMapper;
        this.formVersionMapper = formVersionMapper;
        this.formFieldRuleMapper = formFieldRuleMapper;
        this.formSnapshotMapper = formSnapshotMapper;
        this.auth = auth;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listTemplates(long page, long size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 20 : Math.min(size, 100);
        Long total = formTemplateMapper.countTemplates();
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = formTemplateMapper.selectTemplates(s, (p - 1) * s);
        return new PageResponse<>(p, s, t, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> templateDetail(long id) {
        List<Map<String, Object>> rows = formTemplateMapper.selectTemplateById(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "表单模板不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>(rows.get(0));
        m.put("versions", formVersionMapper.selectVersionsByTemplateId(id));
        m.put("fieldRules", formFieldRuleMapper.selectFieldRulesByTemplateId(id));
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> versionDetail(long versionId) {
        List<Map<String, Object>> rows = formVersionMapper.selectVersionById(versionId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    @Transactional
    public Map<String, Object> createTemplate(FormDtos.TemplateCreateRequest req) {
        Long exists = formTemplateMapper.selectCount(
                new LambdaQueryWrapper<FormTemplate>().eq(FormTemplate::getTemplateCode, req.templateCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "模板编码已存在");
        }
        long id = sequenceService.nextId("form_template");
        FormTemplate entity = new FormTemplate();
        entity.setId(id);
        entity.setTemplateCode(req.templateCode());
        entity.setTemplateName(req.templateName());
        entity.setBusinessType(req.businessType());
        entity.setDescription(req.description());
        entity.setStatus("DRAFT");
        entity.setDeleted(0);
        formTemplateMapper.insert(entity);
        return templateDetail(id);
    }

    @Transactional
    public Map<String, Object> updateTemplate(long id, FormDtos.TemplateUpdateRequest req) {
        templateDetail(id);
        LocalDateTime now = LocalDateTime.now();
        formTemplateMapper.update(null, new LambdaUpdateWrapper<FormTemplate>()
                .eq(FormTemplate::getId, id)
                .set(FormTemplate::getTemplateName, req.templateName())
                .set(FormTemplate::getDescription, req.description())
                .set(FormTemplate::getUpdatedAt, now));
        return templateDetail(id);
    }

    @Transactional
    public Map<String, Object> deleteTemplate(long id) {
        templateDetail(id);
        LocalDateTime now = LocalDateTime.now();
        formTemplateMapper.update(null, new LambdaUpdateWrapper<FormTemplate>()
                .eq(FormTemplate::getId, id)
                .set(FormTemplate::getDeleted, 1)
                .set(FormTemplate::getUpdatedAt, now));
        return Map.of("id", id, "deleted", true);
    }

    @Transactional
    public Map<String, Object> createVersion(long templateId, FormDtos.VersionCreateRequest req) {
        // 校验模板存在
        templateDetail(templateId);
        // 校验 fieldsJson 是合法 JSON 数组
        try {
            var node = json.readTree(req.fieldsJson());
            if (!node.isArray()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "fieldsJson 必须是 JSON 数组");
            }
            for (var f : node) {
                if (!f.has("fieldCode") || !f.has("label")) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "字段必须包含 fieldCode/label");
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "fieldsJson 不是合法 JSON: " + e.getMessage());
        }

        List<Map<String, Object>> maxRows = formVersionMapper.selectVersionsByTemplateId(templateId);
        int nextNo = 1;
        if (!maxRows.isEmpty()) {
            Object vno = maxRows.get(0).get("versionNo");
            if (vno != null) {
                nextNo = ((Number) vno).intValue() + 1;
            }
        }
        long id = sequenceService.nextId("form_version");
        FormVersion entity = new FormVersion();
        entity.setId(id);
        entity.setTemplateId(templateId);
        entity.setVersionNo(nextNo);
        entity.setFieldsJson(req.fieldsJson());
        entity.setLayoutJson(req.layoutJson() == null ? JsonNodeFactory.instance.objectNode().toString() : req.layoutJson());
        entity.setStatus("DRAFT");
        entity.setChangeReason(req.changeReason());
        entity.setCreatedAt(LocalDateTime.now());
        formVersionMapper.insert(entity);
        return versionDetail(id);
    }

    @Transactional
    public Map<String, Object> publishVersion(long versionId) {
        AuthUser user = auth.currentUser();
        Map<String, Object> v = versionDetail(versionId);
        if (!"DRAFT".equals(String.valueOf(v.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅 DRAFT 版本可发布");
        }
        long templateId = ((Number) v.get("templateId")).longValue();
        LocalDateTime now = LocalDateTime.now();
        formVersionMapper.update(null, new LambdaUpdateWrapper<FormVersion>()
                .eq(FormVersion::getId, versionId)
                .eq(FormVersion::getStatus, "DRAFT")
                .set(FormVersion::getStatus, "PUBLISHED")
                .set(FormVersion::getPublishedAt, now)
                .set(FormVersion::getPublishedBy, user.id()));
        // 把同模板其它已发布版本回退为 ARCHIVED
        formVersionMapper.update(null, new LambdaUpdateWrapper<FormVersion>()
                .eq(FormVersion::getTemplateId, templateId)
                .ne(FormVersion::getId, versionId)
                .eq(FormVersion::getStatus, "PUBLISHED")
                .set(FormVersion::getStatus, "ARCHIVED"));
        // 模板状态置为 PUBLISHED 并指向当前版本
        formTemplateMapper.update(null, new LambdaUpdateWrapper<FormTemplate>()
                .eq(FormTemplate::getId, templateId)
                .set(FormTemplate::getStatus, "PUBLISHED")
                .set(FormTemplate::getCurrentVersionId, versionId)
                .set(FormTemplate::getUpdatedAt, now));
        return versionDetail(versionId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> runtime(String businessType) {
        List<Map<String, Object>> rows = formTemplateMapper.selectRuntime(businessType);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到已发布的表单：" + businessType);
        }
        Map<String, Object> m = new LinkedHashMap<>(rows.get(0));
        long templateId = ((Number) m.get("templateId")).longValue();
        m.put("fieldRules", formFieldRuleMapper.selectEnabledRulesByTemplateId(templateId));
        return m;
    }

    @Transactional
    public Map<String, Object> upsertFieldRule(long templateId, FormDtos.FieldRuleUpsertRequest req) {
        templateDetail(templateId);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = formFieldRuleMapper.selectByUniqueKey(templateId, req.fieldCode(), req.ruleType());
        if (!rows.isEmpty()) {
            long existingId = ((Number) rows.get(0).get("id")).longValue();
            formFieldRuleMapper.update(null, new LambdaUpdateWrapper<FormFieldRule>()
                    .eq(FormFieldRule::getId, existingId)
                    .set(FormFieldRule::getRuleExpression, req.ruleExpression())
                    .set(FormFieldRule::getDescription, req.description())
                    .set(FormFieldRule::getUpdatedAt, now));
            return Map.of("id", existingId, "updated", true);
        }
        long id = sequenceService.nextId("form_field_rule");
        FormFieldRule entity = new FormFieldRule();
        entity.setId(id);
        entity.setTemplateId(templateId);
        entity.setFieldCode(req.fieldCode());
        entity.setRuleType(req.ruleType());
        entity.setRuleExpression(req.ruleExpression());
        entity.setDescription(req.description());
        entity.setStatus("ENABLED");
        formFieldRuleMapper.insert(entity);
        return Map.of("id", id, "created", true);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listFieldRules(long page, long size, Long templateId) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 20 : Math.min(size, 100);
        Long total = formFieldRuleMapper.countFieldRules(templateId);
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = formFieldRuleMapper.selectFieldRules(templateId, s, (p - 1) * s);
        return new PageResponse<>(p, s, t, items);
    }

    @Transactional
    public Map<String, Object> saveSnapshot(FormDtos.SnapshotCreateRequest req) {
        Map<String, Object> v = versionDetail(req.versionId());
        long templateId = ((Number) v.get("templateId")).longValue();
        long id = sequenceService.nextId("form_snapshot");
        FormSnapshot entity = new FormSnapshot();
        entity.setId(id);
        entity.setTemplateId(templateId);
        entity.setVersionId(req.versionId());
        entity.setBusinessType(req.businessType());
        entity.setBusinessId(req.businessId());
        entity.setDataJson(req.dataJson());
        entity.setCreatedAt(LocalDateTime.now());
        formSnapshotMapper.insert(entity);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("templateId", templateId);
        m.put("versionId", req.versionId());
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> latestSnapshot(String businessType, long businessId) {
        List<Map<String, Object>> rows = formSnapshotMapper.selectLatestSnapshot(businessType, businessId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "无快照");
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    public long count() {
        return Objects.requireNonNullElse(formTemplateMapper.countTemplates(), 0L);
    }
}
