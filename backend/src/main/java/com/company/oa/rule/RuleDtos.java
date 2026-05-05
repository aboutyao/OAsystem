package com.company.oa.rule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class RuleDtos {
    private RuleDtos() {
    }

    public record RuleCreateRequest(
            @NotNull Long groupId,
            @NotBlank String ruleCode,
            @NotBlank String ruleName,
            @NotBlank String ruleType,
            @NotBlank String businessType,
            String description
    ) {
    }

    public record RuleVersionCreateRequest(
            @NotBlank String ruleContentJson,
            String naturalLanguage,
            String changeReason
    ) {
    }

    public record SimulateRequest(
            @NotBlank String businessType,
            @NotNull Map<String, Object> context
    ) {
    }

    public record RuleUpdateRequest(
            @NotBlank String ruleName,
            String description
    ) {
    }

    public record RuleGroupCreateRequest(
            @NotBlank String groupCode,
            @NotBlank String groupName,
            String description,
            String status
    ) {
    }

    public record RuleGroupUpdateRequest(
            @NotBlank String groupCode,
            @NotBlank String groupName,
            String description,
            String status
    ) {
    }
}
