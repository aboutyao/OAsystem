package com.company.oa.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.rule.RuleAuditLog;
import com.company.oa.entity.rule.RuleDefinition;
import com.company.oa.entity.rule.RuleGroup;
import com.company.oa.entity.rule.RuleVersion;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.rule.mapper.RuleAuditLogMapper;
import com.company.oa.rule.mapper.RuleDefinitionMapper;
import com.company.oa.rule.mapper.RuleGroupMapper;
import com.company.oa.rule.mapper.RuleVersionMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RuleService {
    private static final String ENABLED = "ENABLED";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String DISABLED = "DISABLED";

    private final RuleGroupMapper ruleGroupMapper;
    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final RuleVersionMapper ruleVersionMapper;
    private final RuleAuditLogMapper ruleAuditLogMapper;
    private final SysConfigMapper sysConfigMapper;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final SequenceService sequenceService;

    public RuleService(RuleGroupMapper ruleGroupMapper, RuleDefinitionMapper ruleDefinitionMapper,
                       RuleVersionMapper ruleVersionMapper, RuleAuditLogMapper ruleAuditLogMapper,
                       SysConfigMapper sysConfigMapper, AuthService authService,
                       ObjectMapper objectMapper, SequenceService sequenceService) {
        this.ruleGroupMapper = ruleGroupMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.ruleVersionMapper = ruleVersionMapper;
        this.ruleAuditLogMapper = ruleAuditLogMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRuleGroups() {
        return ruleGroupMapper.selectGroupsWithRuleCount();
    }

    @Transactional
    public Map<String, Object> createRuleGroup(RuleDtos.RuleGroupCreateRequest req) {
        if (groupCodeTaken(req.groupCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "分组编码已存在");
        }
        long id = sequenceService.nextId("rule_group");
        String status = req.status() != null && !req.status().isBlank() ? req.status() : ENABLED;
        RuleGroup entity = new RuleGroup();
        entity.setId(id);
        entity.setGroupCode(req.groupCode());
        entity.setGroupName(req.groupName());
        entity.setDescription(req.description());
        entity.setStatus(status);
        ruleGroupMapper.insert(entity);
        return ruleGroupRow(id);
    }

    @Transactional
    public Map<String, Object> updateRuleGroup(long id, RuleDtos.RuleGroupUpdateRequest req) {
        ruleGroupRow(id);
        if (groupCodeTaken(req.groupCode(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "分组编码已存在");
        }
        String status = req.status() != null && !req.status().isBlank() ? req.status() : ENABLED;
        LocalDateTime now = LocalDateTime.now();
        ruleGroupMapper.update(null, new LambdaUpdateWrapper<RuleGroup>()
                .eq(RuleGroup::getId, id)
                .set(RuleGroup::getGroupCode, req.groupCode())
                .set(RuleGroup::getGroupName, req.groupName())
                .set(RuleGroup::getDescription, req.description())
                .set(RuleGroup::getStatus, status)
                .set(RuleGroup::getUpdatedAt, now));
        return ruleGroupRow(id);
    }

    @Transactional
    public Map<String, Object> deleteRuleGroup(long id) {
        ruleGroupRow(id);
        Long ruleCount = ruleDefinitionMapper.countByGroupId(id);
        if (ruleCount != null && ruleCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该分组下仍有规则，无法删除");
        }
        ruleGroupMapper.deleteById(id);
        return Map.of("id", id, "deleted", true);
    }

    private Map<String, Object> ruleGroupRow(long id) {
        List<Map<String, Object>> rows = ruleGroupMapper.selectGroupById(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规则分组不存在");
        }
        return rows.get(0);
    }

    private boolean groupCodeTaken(String code, Long excludeId) {
        Long n = ruleGroupMapper.countByCode(code, excludeId);
        return n != null && n > 0;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listRules(long page, long size) {
        long[] ps = clampPage(page, size);
        long total = Objects.requireNonNullElse(ruleDefinitionMapper.countRules(), 0L);
        List<Map<String, Object>> items = ruleDefinitionMapper.selectRules(ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ruleDetail(long ruleId) {
        List<Map<String, Object>> rows = ruleDefinitionMapper.selectRuleById(ruleId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规则不存在");
        }
        Map<String, Object> out = new LinkedHashMap<>(rows.get(0));
        out.put("versions", ruleVersionMapper.selectVersionsByRuleId(ruleId));
        return out;
    }

    @Transactional
    public Map<String, Object> createRule(RuleDtos.RuleCreateRequest req) {
        if (!groupExists(req.groupId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "规则分组不存在");
        }
        if (ruleCodeTaken(req.ruleCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "规则编码已存在");
        }
        long id = sequenceService.nextId("rule_definition");
        RuleDefinition entity = new RuleDefinition();
        entity.setId(id);
        entity.setGroupId(req.groupId());
        entity.setRuleCode(req.ruleCode());
        entity.setRuleName(req.ruleName());
        entity.setRuleType(req.ruleType());
        entity.setBusinessType(req.businessType());
        entity.setDescription(req.description());
        entity.setStatus(ENABLED);
        ruleDefinitionMapper.insert(entity);
        insertAudit(id, null, "CREATE", null, null, "新建规则");
        return ruleDetail(id);
    }

    @Transactional
    public Map<String, Object> createVersion(long ruleId, RuleDtos.RuleVersionCreateRequest req) {
        ruleDetail(ruleId);
        int nextVer = Objects.requireNonNullElse(ruleVersionMapper.selectMaxVersionNo(ruleId), 0) + 1;
        long vid = sequenceService.nextId("rule_version");
        validateJson(req.ruleContentJson());
        RuleVersion entity = new RuleVersion();
        entity.setId(vid);
        entity.setRuleId(ruleId);
        entity.setVersionNo(nextVer);
        entity.setRuleContent(req.ruleContentJson());
        entity.setNaturalLanguage(req.naturalLanguage());
        entity.setStatus(DRAFT);
        entity.setChangeReason(req.changeReason());
        ruleVersionMapper.insert(entity);
        insertAudit(ruleId, vid, "CREATE_VERSION", null, null, req.changeReason());
        return versionRow(vid);
    }

    @Transactional
    public Map<String, Object> publishVersion(long versionId) {
        Map<String, Object> ver = versionRow(versionId);
        long ruleId = ((Number) ver.get("ruleId")).longValue();
        if (PUBLISHED.equals(String.valueOf(ver.get("status")))) {
            return ver;
        }
        AuthUser user = authService.currentUser();
        LocalDateTime now = LocalDateTime.now();
        ruleVersionMapper.update(null, new LambdaUpdateWrapper<RuleVersion>()
                .eq(RuleVersion::getRuleId, ruleId)
                .eq(RuleVersion::getStatus, PUBLISHED)
                .set(RuleVersion::getStatus, DISABLED));
        ruleVersionMapper.update(null, new LambdaUpdateWrapper<RuleVersion>()
                .eq(RuleVersion::getId, versionId)
                .set(RuleVersion::getStatus, PUBLISHED)
                .set(RuleVersion::getPublishedBy, user.id())
                .set(RuleVersion::getPublishedAt, now)
                .set(RuleVersion::getEffectiveAt, now));
        ruleDefinitionMapper.update(null, new LambdaUpdateWrapper<RuleDefinition>()
                .eq(RuleDefinition::getId, ruleId)
                .set(RuleDefinition::getUpdatedAt, now));
        insertAudit(ruleId, versionId, "PUBLISH", null, null, "发布版本");
        return versionRow(versionId);
    }

    @Transactional
    public Map<String, Object> updateRule(long ruleId, RuleDtos.RuleUpdateRequest req) {
        ruleDetail(ruleId);
        LocalDateTime now = LocalDateTime.now();
        ruleDefinitionMapper.update(null, new LambdaUpdateWrapper<RuleDefinition>()
                .eq(RuleDefinition::getId, ruleId)
                .set(RuleDefinition::getRuleName, req.ruleName())
                .set(RuleDefinition::getDescription, req.description())
                .set(RuleDefinition::getUpdatedAt, now));
        insertAudit(ruleId, null, "UPDATE", null, null, "更新规则");
        return ruleDetail(ruleId);
    }

    @Transactional
    public Map<String, Object> deleteRule(long ruleId) {
        ruleDetail(ruleId);
        LocalDateTime now = LocalDateTime.now();
        ruleDefinitionMapper.update(null, new LambdaUpdateWrapper<RuleDefinition>()
                .eq(RuleDefinition::getId, ruleId)
                .set(RuleDefinition::getStatus, DISABLED)
                .set(RuleDefinition::getUpdatedAt, now));
        insertAudit(ruleId, null, "DELETE", null, null, "删除规则");
        return Map.of("id", ruleId, "deleted", true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> simulate(long versionId, RuleDtos.SimulateRequest req) {
        Map<String, Object> ver = versionRow(versionId);
        if (!PUBLISHED.equals(String.valueOf(ver.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已发布版本可模拟");
        }
        String content = String.valueOf(ver.get("ruleContent"));
        String businessType = String.valueOf(ver.get("businessType"));
        if (!"GENERIC".equals(businessType) && !businessType.equals(req.businessType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "业务类型与规则不匹配");
        }
        boolean matched = evaluateRuleContent(content, req.context());
        List<Map<String, Object>> actions = new ArrayList<>();
        if (matched) {
            actions.add(Map.of("type", "MATCH", "ruleCode", ver.get("ruleCode"), "versionNo", ver.get("versionNo")));
        }
        return Map.of(
                "matched", matched,
                "matchedRules", matched ? List.of(Map.of("ruleCode", ver.get("ruleCode"), "versionNo", ver.get("versionNo"))) : List.of(),
                "actions", actions
        );
    }

    private boolean evaluateRuleContent(String json, Map<String, Object> context) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String type = root.path("type").asText("");
            if ("AMOUNT".equals(type)) {
                String field = root.path("field").asText("amount");
                String op = root.path("operator").asText(">=");
                double threshold = root.path("value").asDouble(0);
                Object v = context.get(field);
                if (v == null) {
                    return false;
                }
                double val = v instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(v));
                return switch (op) {
                    case ">" -> val > threshold;
                    case ">=" -> val >= threshold;
                    case "<" -> val < threshold;
                    case "<=" -> val <= threshold;
                    case "==" -> Math.abs(val - threshold) < 1e-9;
                    default -> val >= threshold;
                };
            }
            if ("TIME".equals(type)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "规则内容 JSON 无效: " + ex.getMessage());
        }
    }

    private void validateJson(String json) {
        try {
            objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "ruleContent 不是合法 JSON");
        }
    }

    private Map<String, Object> versionRow(long versionId) {
        List<Map<String, Object>> rows = ruleVersionMapper.selectVersionWithRule(versionId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规则版本不存在");
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    private boolean groupExists(long groupId) {
        Long n = ruleGroupMapper.selectCount(
                new LambdaQueryWrapper<RuleGroup>()
                        .eq(RuleGroup::getId, groupId)
                        .eq(RuleGroup::getStatus, ENABLED));
        return n != null && n > 0;
    }

    private boolean ruleCodeTaken(String code) {
        Long n = ruleDefinitionMapper.countByCode(code);
        return n != null && n > 0;
    }

    private void insertAudit(long ruleId, Long versionId, String action, String before, String after, String reason) {
        long id = sequenceService.nextId("rule_audit_log");
        AuthUser user = authService.currentUser();
        RuleAuditLog entity = new RuleAuditLog();
        entity.setId(id);
        entity.setRuleId(ruleId);
        entity.setRuleVersionId(versionId);
        entity.setAction(action);
        entity.setBeforeData(before);
        entity.setAfterData(after);
        entity.setReason(reason);
        entity.setOperatorId(user.id());
        entity.setOperatedAt(LocalDateTime.now());
        ruleAuditLogMapper.insert(entity);
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
