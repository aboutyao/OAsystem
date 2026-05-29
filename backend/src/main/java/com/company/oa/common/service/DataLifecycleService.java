package com.company.oa.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据生命周期服务
 * 自动归档过期数据，自动清理无用数据
 */
@Service
public class DataLifecycleService {
    private static final Logger log = LoggerFactory.getLogger(DataLifecycleService.class);
    private final JdbcTemplate jdbcTemplate;

    public DataLifecycleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 归档过期数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void archiveExpiredData() {
        log.info("开始归档过期数据...");

        // 归档已完成的流程实例（超过6个月）
        LocalDateTime archiveThreshold = LocalDateTime.now().minusMonths(6);
        int archived = jdbcTemplate.update(
            "UPDATE wf_process_instance SET status = 'ARCHIVED' WHERE status = 'COMPLETED' AND ended_at < ?",
            archiveThreshold
        );
        log.info("归档流程实例: {} 条", archived);

        // 归档已完成的请假（超过1年）
        LocalDateTime leaveArchiveThreshold = LocalDateTime.now().minusYears(1);
        int leaveArchived = jdbcTemplate.update(
            "UPDATE oa_leave SET status = 'ARCHIVED' WHERE status IN ('APPROVED', 'COMPLETED') AND created_at < ?",
            leaveArchiveThreshold
        );
        log.info("归档请假记录: {} 条", leaveArchived);
    }

    /**
     * 清理无用数据
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void cleanupUselessData() {
        log.info("开始清理无用数据...");

        // 清理过期的临时文件（超过30天）
        LocalDateTime fileCleanupThreshold = LocalDateTime.now().minusDays(30);
        int filesCleaned = jdbcTemplate.update(
            "UPDATE file_info SET deleted = 1, deleted_at = NOW() WHERE is_temp = 1 AND created_at < ?",
            fileCleanupThreshold
        );
        log.info("清理临时文件: {} 条", filesCleaned);

        // 清理过期的验证码（超过24小时）
        LocalDateTime codeCleanupThreshold = LocalDateTime.now().minusHours(24);
        int codesCleaned = jdbcTemplate.update(
            "DELETE FROM verify_code WHERE created_at < ?",
            codeCleanupThreshold
        );
        log.info("清理验证码: {} 条", codesCleaned);

        // 清理过期的操作日志（超过3个月）
        LocalDateTime logCleanupThreshold = LocalDateTime.now().minusMonths(3);
        int logsCleaned = jdbcTemplate.update(
            "DELETE FROM audit_operation_log WHERE created_at < ?",
            logCleanupThreshold
        );
        log.info("清理操作日志: {} 条", logsCleaned);

        // 清理过期的消息（超过6个月）
        LocalDateTime msgCleanupThreshold = LocalDateTime.now().minusMonths(6);
        int msgsCleaned = jdbcTemplate.update(
            "DELETE FROM msg_message WHERE created_at < ? AND is_read = 1",
            msgCleanupThreshold
        );
        log.info("清理已读消息: {} 条", msgsCleaned);
    }

    /**
     * 获取数据统计
     */
    public Map<String, Object> getDataStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 各表数据量
        stats.put("users", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM org_user WHERE deleted = 0", Long.class));
        stats.put("leaves", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oa_leave WHERE deleted = 0", Long.class));
        stats.put("expenses", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oa_expense WHERE deleted = 0", Long.class));
        stats.put("purchases", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oa_purchase WHERE deleted = 0", Long.class));
        stats.put("contracts", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contract_info WHERE deleted = 0", Long.class));
        stats.put("workflowInstances", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wf_process_instance WHERE deleted = 0", Long.class));

        // 存储空间
        stats.put("totalFileSize", jdbcTemplate.queryForObject("SELECT COALESCE(SUM(file_size), 0) FROM file_info WHERE deleted = 0", Long.class));

        return stats;
    }

    /**
     * 获取归档统计
     */
    public Map<String, Object> getArchiveStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("archivedInstances", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wf_process_instance WHERE status = 'ARCHIVED'", Long.class
        ));
        stats.put("archivedLeaves", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM oa_leave WHERE status = 'ARCHIVED'", Long.class
        ));

        return stats;
    }
}
