package com.company.oa.system;

import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 主题定制服务
 * 企业可自定义主题色、Logo
 */
@Service
public class ThemeService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;

    public ThemeService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 获取主题配置
     */
    public Map<String, Object> getThemeConfig() {
        List<Map<String, Object>> configs = jdbcTemplate.queryForList(
            "SELECT * FROM system_theme_config WHERE status = 'ACTIVE'"
        );

        if (configs.isEmpty()) {
            return getDefaultTheme();
        }

        Map<String, Object> theme = new HashMap<>();
        for (Map<String, Object> config : configs) {
            theme.put(String.valueOf(config.get("config_key")), config.get("config_value"));
        }
        return theme;
    }

    /**
     * 保存主题配置
     */
    public void saveThemeConfig(Map<String, String> config) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            Long existingId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_theme_config WHERE config_key = ?",
                Long.class, entry.getKey()
            );

            if (existingId != null) {
                jdbcTemplate.update(
                    "UPDATE system_theme_config SET config_value = ?, updated_at = NOW() WHERE config_key = ?",
                    entry.getValue(), entry.getKey()
                );
            } else {
                jdbcTemplate.update(
                    "INSERT INTO system_theme_config (id, config_key, config_value, status, created_at) VALUES (?, ?, ?, 'ACTIVE', NOW())",
                    sequenceService.nextId("system_theme_config"), entry.getKey(), entry.getValue()
                );
            }
        }
    }

    /**
     * 获取预设主题列表
     */
    public List<Map<String, Object>> getPresetThemes() {
        return List.of(
            Map.of("name", "默认蓝", "primary", "#409EFF", "sidebar", "#0f172a"),
            Map.of("name", "活力橙", "primary", "#E6A23C", "sidebar", "#1a1a2e"),
            Map.of("name", "商务灰", "primary", "#909399", "sidebar", "#2d3436"),
            Map.of("name", "自然绿", "primary", "#67C23A", "sidebar", "#0d3b2e"),
            Map.of("name", "热情红", "primary", "#F56C6C", "sidebar", "#2d132c")
        );
    }

    private Map<String, Object> getDefaultTheme() {
        Map<String, Object> theme = new HashMap<>();
        theme.put("primary", "#4f46e5");
        theme.put("sidebar", "#0f172a");
        theme.put("header", "#ffffff");
        theme.put("borderRadius", "8px");
        theme.put("logo", "");
        theme.put("title", "企业级 OA 系统");
        return theme;
    }
}
