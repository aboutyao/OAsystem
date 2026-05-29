package com.company.oa.dashboard;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 个性化仪表盘服务
 * 每人可自定义首页展示哪些模块
 */
@Service
public class PersonalDashboardService {
    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;

    public PersonalDashboardService(JdbcTemplate jdbcTemplate, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
    }

    /**
     * 获取用户仪表盘配置
     */
    public Map<String, Object> getDashboardConfig(long userId) {
        List<Map<String, Object>> configs = jdbcTemplate.queryForList(
            "SELECT * FROM user_dashboard_config WHERE user_id = ? ORDER BY sort_order",
            userId
        );

        if (configs.isEmpty()) {
            // 返回默认配置
            return getDefaultConfig();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("modules", configs);
        return result;
    }

    /**
     * 保存用户仪表盘配置
     */
    public void saveDashboardConfig(long userId, List<Map<String, Object>> modules) {
        // 删除现有配置
        jdbcTemplate.update("DELETE FROM user_dashboard_config WHERE user_id = ?", userId);

        // 保存新配置
        int sortOrder = 0;
        for (Map<String, Object> module : modules) {
            jdbcTemplate.update(
                "INSERT INTO user_dashboard_config (id, user_id, module_key, module_name, visible, sort_order, config_data, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                generateId(), userId, module.get("key"), module.get("name"),
                module.getOrDefault("visible", true), sortOrder++,
                module.get("config")
            );
        }
    }

    /**
     * 获取快捷入口配置（按使用频率排序）
     */
    public List<Map<String, Object>> getQuickActions(long userId) {
        // 获取用户使用统计
        List<Map<String, Object>> usageStats = jdbcTemplate.queryForList(
            "SELECT action_path, COUNT(*) as use_count FROM user_action_log WHERE user_id = ? GROUP BY action_path ORDER BY use_count DESC LIMIT 10",
            userId
        );

        if (usageStats.isEmpty()) {
            return getDefaultQuickActions();
        }

        return usageStats;
    }

    /**
     * 记录用户操作
     */
    public void logUserAction(long userId, String actionPath) {
        jdbcTemplate.update(
            "INSERT INTO user_action_log (id, user_id, action_path, created_at) VALUES (?, ?, ?, NOW())",
            generateId(), userId, actionPath
        );
    }

    private Map<String, Object> getDefaultConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("modules", List.of(
            Map.of("key", "todos", "name", "我的待办", "visible", true),
            Map.of("key", "started", "name", "我发起的", "visible", true),
            Map.of("key", "messages", "name", "消息中心", "visible", true),
            Map.of("key", "calendar", "name", "日程", "visible", true),
            Map.of("key", "leave-balance", "name", "假期余额", "visible", true),
            Map.of("key", "quick-actions", "name", "快捷入口", "visible", true)
        ));
        return config;
    }

    private List<Map<String, Object>> getDefaultQuickActions() {
        return List.of(
            Map.of("path", "/oa/leaves/create", "label", "新建请假", "icon", "Calendar"),
            Map.of("path", "/oa/expenses/create", "label", "新建报销", "icon", "Wallet"),
            Map.of("path", "/oa/purchases/create", "label", "新建采购", "icon", "ShoppingCart"),
            Map.of("path", "/todos", "label", "我的待办", "icon", "Document")
        );
    }

    private Long generateId() {
        return System.currentTimeMillis() * 1000 + new Random().nextInt(1000);
    }
}
