package com.company.oa.template;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 模板市场服务
 * 预置行业模板：IT、金融、制造业
 */
@Service
public class TemplateMarketService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final AuthService authService;

    public TemplateMarketService(JdbcTemplate jdbcTemplate, SequenceService sequenceService, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
        this.authService = authService;
    }

    /**
     * 获取模板列表
     */
    public List<Map<String, Object>> listTemplates(String category, String industry) {
        StringBuilder sql = new StringBuilder("SELECT * FROM template_market WHERE status = 'ACTIVE'");
        List<Object> params = new ArrayList<>();

        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (industry != null && !industry.isEmpty()) {
            sql.append(" AND industry = ?");
            params.add(industry);
        }

        sql.append(" ORDER BY download_count DESC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 获取模板详情
     */
    public Map<String, Object> getTemplateDetail(long templateId) {
        return jdbcTemplate.queryForMap("SELECT * FROM template_market WHERE id = ?", templateId);
    }

    /**
     * 下载模板
     */
    @Transactional
    public Map<String, Object> downloadTemplate(long templateId) {
        AuthUser user = authService.currentUser();

        // 更新下载次数
        jdbcTemplate.update(
            "UPDATE template_market SET download_count = download_count + 1 WHERE id = ?",
            templateId
        );

        // 记录下载
        long id = sequenceService.nextId("template_download");
        jdbcTemplate.update(
            "INSERT INTO template_download (id, template_id, user_id, downloaded_at) VALUES (?, ?, ?, NOW())",
            id, templateId, user.id()
        );

        // 获取模板内容
        Map<String, Object> template = jdbcTemplate.queryForMap(
            "SELECT * FROM template_market WHERE id = ?", templateId
        );

        return Map.of(
            "templateId", templateId,
            "content", template.get("content"),
            "config", template.get("config")
        );
    }

    /**
     * 创建模板
     */
    @Transactional
    public Map<String, Object> createTemplate(String name, String description, String category, String industry, String content, String config) {
        AuthUser user = authService.currentUser();
        long id = sequenceService.nextId("template_market");

        jdbcTemplate.update(
            "INSERT INTO template_market (id, name, description, category, industry, content, config, author_id, author_name, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW())",
            id, name, description, category, industry, content, config, user.id(), user.realName()
        );

        return Map.of("id", id, "status", "ACTIVE");
    }

    /**
     * 获取模板分类
     */
    public List<Map<String, Object>> getCategories() {
        return jdbcTemplate.queryForList(
            "SELECT category, COUNT(*) as count FROM template_market WHERE status = 'ACTIVE' GROUP BY category"
        );
    }

    /**
     * 获取行业列表
     */
    public List<Map<String, Object>> getIndustries() {
        return jdbcTemplate.queryForList(
            "SELECT industry, COUNT(*) as count FROM template_market WHERE status = 'ACTIVE' GROUP BY industry"
        );
    }

    /**
     * 获取推荐模板
     */
    public List<Map<String, Object>> getRecommendTemplates(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM template_market WHERE status = 'ACTIVE' ORDER BY download_count DESC LIMIT ?",
            limit
        );
    }
}
