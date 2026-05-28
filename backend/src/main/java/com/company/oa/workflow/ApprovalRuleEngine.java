package com.company.oa.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.org.User;
import com.company.oa.entity.wf.WfApprovalRule;
import com.company.oa.org.mapper.DeptMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.mapper.WfApprovalRuleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    public ApprovalRuleEngine(WfApprovalRuleMapper ruleMapper, UserMapper userMapper,
                              DeptMapper deptMapper, ObjectMapper objectMapper) {
        this.ruleMapper = ruleMapper;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.objectMapper = objectMapper;
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
}
