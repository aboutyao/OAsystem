package com.company.oa.workflow;

import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 流程版本管理服务
 * 流程变更不影响已发起的单据
 */
@Service
public class ProcessVersionService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;

    public ProcessVersionService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 获取流程模板的所有版本
     */
    public List<Map<String, Object>> listVersions(long templateId) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM wf_process_version WHERE template_id = ? ORDER BY version_number DESC",
            templateId
        );
    }

    /**
     * 获取最新版本
     */
    public Map<String, Object> getLatestVersion(long templateId) {
        List<Map<String, Object>> versions = jdbcTemplate.queryForList(
            "SELECT * FROM wf_process_version WHERE template_id = ? AND status = 'ACTIVE' ORDER BY version_number DESC LIMIT 1",
            templateId
        );
        return versions.isEmpty() ? null : versions.get(0);
    }

    /**
     * 创建新版本
     */
    @Transactional
    public Map<String, Object> createVersion(long templateId, String config, String changeDescription) {
        // 获取当前最大版本号
        Long maxVersion = jdbcTemplate.queryForObject(
            "SELECT MAX(version_number) FROM wf_process_version WHERE template_id = ?",
            Long.class, templateId
        );

        int newVersionNumber = (maxVersion != null ? maxVersion.intValue() : 0) + 1;

        long id = sequenceService.nextId("wf_process_version");
        jdbcTemplate.update(
            "INSERT INTO wf_process_version (id, template_id, version_number, config, change_description, status, created_at) VALUES (?, ?, ?, ?, ?, 'DRAFT', NOW())",
            id, templateId, newVersionNumber, config, changeDescription
        );

        return Map.of("id", id, "versionNumber", newVersionNumber, "status", "DRAFT");
    }

    /**
     * 发布版本
     */
    @Transactional
    public Map<String, Object> publishVersion(long versionId) {
        // 获取版本信息
        Map<String, Object> version = jdbcTemplate.queryForMap(
            "SELECT * FROM wf_process_version WHERE id = ?", versionId
        );

        long templateId = ((Number) version.get("template_id")).longValue();

        // 将其他版本设为非活跃
        jdbcTemplate.update(
            "UPDATE wf_process_version SET status = 'INACTIVE' WHERE template_id = ? AND status = 'ACTIVE'",
            templateId
        );

        // 发布当前版本
        jdbcTemplate.update(
            "UPDATE wf_process_version SET status = 'ACTIVE', published_at = NOW() WHERE id = ?",
            versionId
        );

        // 更新模板的当前版本
        jdbcTemplate.update(
            "UPDATE wf_process_template SET current_version_id = ? WHERE id = ?",
            versionId, templateId
        );

        return Map.of("status", "ACTIVE");
    }

    /**
     * 获取版本对比
     */
    public Map<String, Object> compareVersions(long versionId1, long versionId2) {
        Map<String, Object> v1 = jdbcTemplate.queryForMap("SELECT * FROM wf_process_version WHERE id = ?", versionId1);
        Map<String, Object> v2 = jdbcTemplate.queryForMap("SELECT * FROM wf_process_version WHERE id = ?", versionId2);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("version1", v1);
        comparison.put("version2", v2);
        comparison.put("configChanged", !v1.get("config").equals(v2.get("config")));
        comparison.put("descriptionChanged", !v1.get("change_description").equals(v2.get("change_description")));

        return comparison;
    }

    /**
     * 获取使用某版本的流程实例数量
     */
    public long getInstanceCount(long versionId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wf_process_instance WHERE process_version_id = ?",
            Long.class, versionId
        );
        return count != null ? count : 0;
    }

    /**
     * 回滚到指定版本
     */
    @Transactional
    public Map<String, Object> rollbackToVersion(long versionId) {
        Map<String, Object> version = jdbcTemplate.queryForMap(
            "SELECT * FROM wf_process_version WHERE id = ?", versionId
        );

        long templateId = ((Number) version.get("template_id")).longValue();

        // 创建新版本（复制旧版本配置）
        String config = (String) version.get("config");
        String description = "回滚到版本 " + version.get("version_number");

        return createVersion(templateId, config, description);
    }
}
