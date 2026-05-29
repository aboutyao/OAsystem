package com.company.oa.workflow;

import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 自动化工作流服务
 * 条件触发、定时任务
 */
@Service
public class AutomationService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;

    public AutomationService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 注册自动化规则
     */
    public Map<String, Object> registerRule(String name, String triggerType, String condition, String action) {
        long id = sequenceService.nextId("automation_rule");
        jdbcTemplate.update(
            "INSERT INTO automation_rule (id, name, trigger_type, condition_expr, action_expr, status, created_at) VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW())",
            id, name, triggerType, condition, action
        );
        return Map.of("id", id, "status", "ACTIVE");
    }

    /**
     * 检查并触发自动化规则
     */
    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    public void checkAndTriggerRules() {
        List<Map<String, Object>> rules = jdbcTemplate.queryForList(
            "SELECT * FROM automation_rule WHERE status = 'ACTIVE'"
        );

        for (Map<String, Object> rule : rules) {
            try {
                String triggerType = (String) rule.get("trigger_type");
                String condition = (String) rule.get("condition_expr");
                String action = (String) rule.get("action_expr");

                // 检查条件是否满足
                if (evaluateCondition(triggerType, condition)) {
                    executeAction(action, rule);
                    recordExecution((Long) rule.get("id"), "SUCCESS");
                }
            } catch (Exception e) {
                recordExecution((Long) rule.get("id"), "FAILED");
            }
        }
    }

    private boolean evaluateCondition(String triggerType, String condition) {
        // 简化实现：根据触发类型检查条件
        return switch (triggerType) {
            case "CONTRACT_EXPIRING" -> checkContractExpiring(condition);
            case "BUDGET_EXCEEDED" -> checkBudgetExceeded(condition);
            case "LEAVE_BALANCE_LOW" -> checkLeaveBalanceLow(condition);
            default -> false;
        };
    }

    private boolean checkContractExpiring(String condition) {
        // 检查即将到期的合同
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM contract_info WHERE end_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY) AND status = 'ACTIVE'",
            Long.class
        );
        return count != null && count > 0;
    }

    private boolean checkBudgetExceeded(String condition) {
        // 检查超支预算
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM oa_budget WHERE used_amount > budget_amount * 0.9 AND status = 'ACTIVE'",
            Long.class
        );
        return count != null && count > 0;
    }

    private boolean checkLeaveBalanceLow(String condition) {
        // 检查余额不足
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM oa_leave_balance WHERE remaining_days < 3 AND year = YEAR(NOW())",
            Long.class
        );
        return count != null && count > 0;
    }

    private void executeAction(String action, Map<String, Object> rule) {
        // 根据动作类型执行
        switch (action) {
            case "NOTIFY" -> sendNotification(rule);
            case "CREATE_TASK" -> createAutoTask(rule);
            case "UPDATE_STATUS" -> updateStatus(rule);
        }
    }

    private void sendNotification(Map<String, Object> rule) {
        // 发送通知
        jdbcTemplate.update(
            "INSERT INTO msg_message (id, sender_id, sender_name, receiver_id, receiver_name, title, content, msg_type, is_read, created_at) VALUES (?, 0, '系统', 0, '全部', '自动化通知', ?, 'SYSTEM', 0, NOW())",
            generateId(), "自动化规则触发: " + rule.get("name")
        );
    }

    private void createAutoTask(Map<String, Object> rule) {
        // 创建自动任务
    }

    private void updateStatus(Map<String, Object> rule) {
        // 更新状态
    }

    private void recordExecution(Long ruleId, String status) {
        try {
            jdbcTemplate.update(
                "INSERT INTO automation_execution (id, rule_id, status, executed_at) VALUES (?, ?, ?, NOW())",
                generateId(), ruleId, status
            );
        } catch (Exception e) {
            // 记录失败不影响主流程
        }
    }

    /**
     * 获取自动化规则列表
     */
    public List<Map<String, Object>> listRules() {
        return jdbcTemplate.queryForList("SELECT * FROM automation_rule WHERE status = 'ACTIVE'");
    }

    /**
     * 获取执行历史
     */
    public List<Map<String, Object>> getExecutionHistory(Long ruleId) {
        return jdbcTemplate.queryForList(
            "SELECT e.*, r.name as rule_name FROM automation_execution e LEFT JOIN automation_rule r ON e.rule_id = r.id WHERE e.rule_id = ? ORDER BY e.executed_at DESC",
            ruleId
        );
    }

    private Long generateId() {
        return System.currentTimeMillis() * 1000 + new Random().nextInt(1000);
    }
}
