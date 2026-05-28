package com.company.oa.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.org.User;
import com.company.oa.entity.wf.WfApprovalRule;
import com.company.oa.org.mapper.DeptMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.mapper.WfApprovalRuleMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import com.company.oa.workflow.mapper.WfTaskRecordMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 动态审批规则引擎
 * 根据业务类型和条件（金额/天数）动态选择审批链
 */
@Service
public class ApprovalRuleEngine {
    private static final Logger log = LoggerFactory.getLogger(ApprovalRuleEngine.class);
    private final WfApprovalRuleMapper ruleMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final ObjectMapper objectMapper;
    private final WfTaskMapper wfTaskMapper;
    private final WfTaskRecordMapper wfTaskRecordMapper;

    public ApprovalRuleEngine(WfApprovalRuleMapper ruleMapper, UserMapper userMapper,
                              DeptMapper deptMapper, ObjectMapper objectMapper,
                              WfTaskMapper wfTaskMapper, WfTaskRecordMapper wfTaskRecordMapper) {
        this.ruleMapper = ruleMapper;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.objectMapper = objectMapper;
        this.wfTaskMapper = wfTaskMapper;
        this.wfTaskRecordMapper = wfTaskRecordMapper;
    }

    /**
     * 根据业务类型和条件值，计算审批链
     */
    public List<Long> resolveApprovalChain(String businessType, BigDecimal conditionValue, long starterId) {
        List<WfApprovalRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<WfApprovalRule>()
                        .eq(WfApprovalRule::getBusinessType, businessType)
                        .eq(WfApprovalRule::getStatus, "ENABLED")
                        .orderByDesc(WfApprovalRule::getPriority)
        );

        if (rules.isEmpty()) {
            log.warn("No approval rule found for business type: {}, using default", businessType);
            return getDefaultChain(starterId);
        }

        WfApprovalRule matchedRule = null;
        for (WfApprovalRule rule : rules) {
            if (matchCondition(rule, conditionValue)) {
                matchedRule = rule;
                break;
            }
        }

        if (matchedRule == null) {
            log.warn("No rule matched for business type: {}, value: {}", businessType, conditionValue);
            return getDefaultChain(starterId);
        }

        return buildApprovalChain(matchedRule.getApprovalChain(), starterId);
    }

    private boolean matchCondition(WfApprovalRule rule, BigDecimal conditionValue) {
        String conditionType = rule.getConditionType();
        if ("DEFAULT".equals(conditionType)) {
            return true;
        }
        if (conditionValue == null) {
            return "DEFAULT".equals(conditionType);
        }
        BigDecimal threshold = rule.getConditionValue();
        if (threshold == null) {
            return true;
        }
        return switch (conditionType) {
            case "AMOUNT" -> conditionValue.compareTo(threshold) <= 0;
            case "DAYS" -> conditionValue.compareTo(threshold) <= 0;
            default -> true;
        };
    }

    private List<Long> buildApprovalChain(String approvalChainJson, long starterId) {
        try {
            List<String> nodeTypes = objectMapper.readValue(approvalChainJson, new TypeReference<>() {});
            List<Long> approverIds = new ArrayList<>();
            User starter = userMapper.selectById(starterId);

            for (String nodeType : nodeTypes) {
                Long approverId = resolveNode(nodeType, starter);
                if (approverId != null && approverId != starterId) {
                    approverIds.add(approverId);
                }
            }
            return approverIds;
        } catch (Exception e) {
            log.error("Failed to parse approval chain: {}", approvalChainJson, e);
            return getDefaultChain(starterId);
        }
    }

    private Long resolveNode(String nodeType, User starter) {
        return switch (nodeType) {
            case "DIRECT_SUPERVISOR" -> starter.getManagerUserId();
            case "DEPARTMENT_HEAD" -> starter.getManagerUserId(); // 简化实现
            case "HR" -> findUserByRole("HR");
            case "GM" -> findUserByRole("GM");
            case "CEO" -> findUserByRole("CEO");
            case "FINANCE" -> findUserByRole("FINANCE");
            case "LEGAL" -> findUserByRole("LEGAL");
            default -> null;
        };
    }

    private Long findUserByRole(String roleCode) {
        Long userId = userMapper.selectUserIdByRoleCode(roleCode);
        if (userId == null) {
            log.warn("No user found with role code: {}", roleCode);
        }
        return userId;
    }

    private List<Long> getDefaultChain(long starterId) {
        List<Long> chain = new ArrayList<>();
        User starter = userMapper.selectById(starterId);
        if (starter != null && starter.getManagerUserId() != null) {
            chain.add(starter.getManagerUserId());
        }
        return chain;
    }

    /**
     * Smart approval routing based on historical response time and current workload.
     * Finds all users with the required role, scores them by responsiveness and capacity,
     * and returns the chain sorted by score (best approver first).
     *
     * Score formula: score = 1 / (avgResponseHours * workloadFactor)
     * where workloadFactor = 1 + (pendingTasks / 10) to penalize overloaded approvers
     *
     * @param businessType the business type to determine which role is needed
     * @param conditionValue the condition value (e.g., amount, days)
     * @param starterId the user initiating the approval
     * @return list of approver user IDs sorted by score (best first)
     */
    public List<Long> resolveSmartChain(String businessType, BigDecimal conditionValue, long starterId) {
        // First, resolve the role requirement from the static rule
        String requiredRole = resolveRequiredRoleFromRule(businessType, conditionValue);

        // Find all users with the required role
        List<Long> candidateIds = userMapper.selectAllUserIdsByRoleCode(requiredRole);
        if (candidateIds == null || candidateIds.isEmpty()) {
            log.warn("Smart routing: no candidates found for role '{}', falling back to static chain",
                    requiredRole);
            return resolveApprovalChain(businessType, conditionValue, starterId);
        }

        // Remove starter from candidates
        candidateIds.removeIf(id -> id == starterId);

        if (candidateIds.isEmpty()) {
            log.warn("Smart routing: only candidate is the starter, falling back to static chain");
            return resolveApprovalChain(businessType, conditionValue, starterId);
        }

        // Score each candidate based on response time and workload
        List<ApproverCandidate> candidates = new ArrayList<>();

        for (Long userId : candidateIds) {
            // Get average response time from wf_task_record
            Map<String, Object> stats = wfTaskRecordMapper.selectApprovalStats(userId);
            double avgResponseHours = 24.0; // default: 24 hours if no history
            long totalApprovals = 0;

            if (stats != null && stats.get("avgResponseHours") != null) {
                avgResponseHours = ((Number) stats.get("avgResponseHours")).doubleValue();
                avgResponseHours = Math.max(avgResponseHours, 0.1); // floor at 6 minutes
            }
            if (stats != null && stats.get("totalApprovals") != null) {
                totalApprovals = ((Number) stats.get("totalApprovals")).longValue();
            }

            // Get current pending task count (workload)
            Long pendingCount = wfTaskMapper.countTodoTasks(userId, "PENDING");
            long currentWorkload = pendingCount != null ? pendingCount : 0L;

            // Calculate workload factor: penalize overloaded approvers
            // Formula: 1 + (pendingTasks / 10)
            // e.g., 0 pending = 1.0x, 5 pending = 1.5x, 10 pending = 2.0x
            double workloadFactor = 1.0 + (currentWorkload / 10.0);

            // Calculate score: higher is better
            // score = 1 / (avgResponseHours * workloadFactor)
            double score = 1.0 / (avgResponseHours * workloadFactor);

            // If user has no approval history, apply a penalty (prefer experienced approvers)
            if (totalApprovals == 0) {
                score *= 0.5;
            }

            // Get user name for logging
            Map<String, Object> userSnapshot = userMapper.selectUserSnapshot(userId);
            String userName = userSnapshot != null
                    ? (String) userSnapshot.getOrDefault("realName", "Unknown")
                    : "Unknown";

            candidates.add(new ApproverCandidate(userId, userName, avgResponseHours,
                    currentWorkload, totalApprovals, score));
        }

        // Sort by score descending (best first)
        candidates.sort(Comparator.comparingDouble(ApproverCandidate::score).reversed());

        // Log the ranking for debugging
        if (log.isDebugEnabled()) {
            log.debug("Smart routing for role '{}':", requiredRole);
            for (int i = 0; i < candidates.size(); i++) {
                ApproverCandidate c = candidates.get(i);
                log.debug("  #{}: {} (id={}) — score={}, avgResponse={}h, workload={}, totalApprovals={}",
                        i + 1, c.name, c.userId,
                        String.format("%.4f", c.score),
                        String.format("%.1f", c.avgResponseHours),
                        c.currentWorkload,
                        c.totalApprovals);
            }
        }

        return candidates.stream()
                .map(c -> c.userId)
                .toList();
    }

    /**
     * Determine which role code is required for a given business type and condition.
     * Parses the matched rule's approval chain to extract the first role node.
     */
    private String resolveRequiredRoleFromRule(String businessType, BigDecimal conditionValue) {
        List<WfApprovalRule> rules = ruleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfApprovalRule>()
                        .eq(WfApprovalRule::getBusinessType, businessType)
                        .eq(WfApprovalRule::getStatus, "ENABLED")
                        .orderByDesc(WfApprovalRule::getPriority)
        );

        for (WfApprovalRule rule : rules) {
            if (matchCondition(rule, conditionValue)) {
                try {
                    List<String> nodeTypes = objectMapper.readValue(
                            rule.getApprovalChain(), new TypeReference<>() {});
                    // Return the first role-based node (skip DIRECT_SUPERVISOR as it's person-specific)
                    for (String node : nodeTypes) {
                        if (!"DIRECT_SUPERVISOR".equals(node) && !"DEPARTMENT_HEAD".equals(node)) {
                            return node;
                        }
                    }
                    // If chain only has supervisor/head, fall back to HR
                    return "HR";
                } catch (Exception e) {
                    log.error("Failed to parse approval chain for smart routing", e);
                    return "HR";
                }
            }
        }

        // Default to HR if no rule matched
        return "HR";
    }

    /**
     * Internal representation of a scored approver candidate.
     */
    private static class ApproverCandidate {
        final long userId;
        final String name;
        final double avgResponseHours;
        final long currentWorkload;
        final long totalApprovals;
        final double score;

        ApproverCandidate(long userId, String name, double avgResponseHours,
                          long currentWorkload, long totalApprovals, double score) {
            this.userId = userId;
            this.name = name;
            this.avgResponseHours = avgResponseHours;
            this.currentWorkload = currentWorkload;
            this.totalApprovals = totalApprovals;
            this.score = score;
        }

        public double score() { return score; }
    }
}
