package com.company.oa.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据同步服务
 * 与ERP、财务、考勤系统自动同步
 */
@Service
public class DataSyncService {
    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);
    private final JdbcTemplate jdbcTemplate;

    public DataSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 同步员工数据
     */
    public Map<String, Object> syncEmployeeData() {
        log.info("开始同步员工数据...");
        try {
            // 从外部系统获取员工数据
            List<Map<String, Object>> externalEmployees = fetchExternalEmployees();

            int synced = 0;
            int updated = 0;

            for (Map<String, Object> emp : externalEmployees) {
                String externalId = (String) emp.get("external_id");
                String name = (String) emp.get("name");
                String department = (String) emp.get("department");

                // 检查是否已存在
                Long existingId = jdbcTemplate.queryForObject(
                    "SELECT id FROM org_user WHERE external_id = ?",
                    Long.class, externalId
                );

                if (existingId == null) {
                    // 新增员工
                    jdbcTemplate.update(
                        "INSERT INTO org_user (id, username, real_name, main_dept_name, external_id, sync_time, created_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                        generateId(), externalId, name, department, externalId
                    );
                    synced++;
                } else {
                    // 更新员工信息
                    jdbcTemplate.update(
                        "UPDATE org_user SET real_name = ?, main_dept_name = ?, sync_time = NOW() WHERE id = ?",
                        name, department, existingId
                    );
                    updated++;
                }
            }

            return Map.of("synced", synced, "updated", updated, "total", externalEmployees.size());
        } catch (Exception e) {
            log.error("同步员工数据失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 同步考勤数据
     */
    public Map<String, Object> syncAttendanceData(LocalDateTime date) {
        log.info("开始同步考勤数据: {}", date);
        try {
            List<Map<String, Object>> attendanceRecords = fetchExternalAttendance(date);

            int synced = 0;
            for (Map<String, Object> record : attendanceRecords) {
                String employeeId = (String) record.get("employee_id");
                String checkInTime = (String) record.get("check_in_time");
                String checkOutTime = (String) record.get("check_out_time");

                // 查找内部用户ID
                Long userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM org_user WHERE external_id = ?",
                    Long.class, employeeId
                );

                if (userId != null) {
                    // 保存考勤记录
                    jdbcTemplate.update(
                        "INSERT INTO attendance_record (id, user_id, check_in_time, check_out_time, record_date, created_at) VALUES (?, ?, ?, ?, ?, NOW())",
                        generateId(), userId, checkInTime, checkOutTime, date.toLocalDate()
                    );
                    synced++;
                }
            }

            return Map.of("synced", synced, "total", attendanceRecords.size());
        } catch (Exception e) {
            log.error("同步考勤数据失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 同步财务数据
     */
    public Map<String, Object> syncFinanceData() {
        log.info("开始同步财务数据...");
        try {
            // 同步报销状态
            List<Map<String, Object>> expenseStatuses = fetchExternalExpenseStatus();

            int synced = 0;
            for (Map<String, Object> status : expenseStatuses) {
                String externalId = (String) status.get("external_id");
                String paymentStatus = (String) status.get("payment_status");

                jdbcTemplate.update(
                    "UPDATE oa_expense SET payment_status = ?, paid_at = CASE WHEN ? = 'PAID' THEN NOW() ELSE paid_at END WHERE expense_no = ?",
                    paymentStatus, paymentStatus, externalId
                );
                synced++;
            }

            return Map.of("synced", synced);
        } catch (Exception e) {
            log.error("同步财务数据失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    // 外部系统接口（需要实际实现）
    private List<Map<String, Object>> fetchExternalEmployees() {
        // TODO: 调用外部员工系统API
        return Collections.emptyList();
    }

    private List<Map<String, Object>> fetchExternalAttendance(LocalDateTime date) {
        // TODO: 调用外部考勤系统API
        return Collections.emptyList();
    }

    private List<Map<String, Object>> fetchExternalExpenseStatus() {
        // TODO: 调用外部财务系统API
        return Collections.emptyList();
    }

    /**
     * 获取同步状态
     */
    public Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();

        // 获取最近同步时间
        LocalDateTime lastEmployeeSync = jdbcTemplate.queryForObject(
            "SELECT MAX(sync_time) FROM org_user WHERE sync_time IS NOT NULL",
            LocalDateTime.class
        );
        status.put("lastEmployeeSync", lastEmployeeSync);

        return status;
    }

    private Long generateId() {
        return System.currentTimeMillis() * 1000 + new java.util.Random().nextInt(1000);
    }
}
