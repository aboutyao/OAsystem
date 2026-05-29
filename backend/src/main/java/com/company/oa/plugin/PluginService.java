package com.company.oa.plugin;

import com.company.oa.common.service.SequenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 插件系统服务
 * 支持自定义插件扩展功能
 */
@Service
public class PluginService {
    private static final Logger log = LoggerFactory.getLogger(PluginService.class);
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;
    private final Map<String, Plugin> loadedPlugins = new HashMap<>();

    public PluginService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 注册插件
     */
    @Transactional
    public Map<String, Object> registerPlugin(String name, String version, String description, String author, String pluginClass) {
        long id = sequenceService.nextId("plugin");

        jdbcTemplate.update(
            "INSERT INTO plugin (id, name, version, description, author, plugin_class, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'REGISTERED', NOW())",
            id, name, version, description, author, pluginClass
        );

        return Map.of("id", id, "status", "REGISTERED");
    }

    /**
     * 启用插件
     */
    @Transactional
    public Map<String, Object> enablePlugin(long pluginId) {
        try {
            // 获取插件信息
            Map<String, Object> plugin = jdbcTemplate.queryForMap(
                "SELECT * FROM plugin WHERE id = ?", pluginId
            );

            String pluginClass = (String) plugin.get("plugin_class");

            // 实例化插件
            Class<?> clazz = Class.forName(pluginClass);
            Plugin pluginInstance = (Plugin) clazz.getDeclaredConstructor().newInstance();
            pluginInstance.onEnable();

            loadedPlugins.put(pluginClass, pluginInstance);

            // 更新状态
            jdbcTemplate.update(
                "UPDATE plugin SET status = 'ENABLED', enabled_at = NOW() WHERE id = ?",
                pluginId
            );

            log.info("插件已启用: {}", plugin.get("name"));
            return Map.of("status", "ENABLED");
        } catch (Exception e) {
            log.error("启用插件失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 禁用插件
     */
    @Transactional
    public Map<String, Object> disablePlugin(long pluginId) {
        try {
            Map<String, Object> plugin = jdbcTemplate.queryForMap(
                "SELECT * FROM plugin WHERE id = ?", pluginId
            );

            String pluginClass = (String) plugin.get("plugin_class");
            Plugin pluginInstance = loadedPlugins.remove(pluginClass);

            if (pluginInstance != null) {
                pluginInstance.onDisable();
            }

            jdbcTemplate.update(
                "UPDATE plugin SET status = 'DISABLED', disabled_at = NOW() WHERE id = ?",
                pluginId
            );

            log.info("插件已禁用: {}", plugin.get("name"));
            return Map.of("status", "DISABLED");
        } catch (Exception e) {
            log.error("禁用插件失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取插件列表
     */
    public List<Map<String, Object>> listPlugins() {
        return jdbcTemplate.queryForList("SELECT * FROM plugin ORDER BY created_at DESC");
    }

    /**
     * 获取已启用的插件
     */
    public List<Plugin> getEnabledPlugins() {
        return new ArrayList<>(loadedPlugins.values());
    }

    /**
     * 执行插件钩子
     */
    public void executeHook(String hookName, Map<String, Object> context) {
        for (Plugin plugin : loadedPlugins.values()) {
            try {
                plugin.onHook(hookName, context);
            } catch (Exception e) {
                log.error("执行插件钩子失败: {} -> {}", plugin.getName(), hookName, e);
            }
        }
    }

    /**
     * 插件接口
     */
    public interface Plugin {
        String getName();
        String getVersion();
        void onEnable();
        void onDisable();
        void onHook(String hookName, Map<String, Object> context);
    }
}
