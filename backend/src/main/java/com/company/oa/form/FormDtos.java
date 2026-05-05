package com.company.oa.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class FormDtos {
    private FormDtos() {
    }

    public record TemplateCreateRequest(
            @NotBlank String templateCode,
            @NotBlank String templateName,
            @NotBlank String businessType,
            String description
    ) {
    }

    public record VersionCreateRequest(
            @NotBlank String fieldsJson,
            String layoutJson,
            String changeReason
    ) {
    }

    public record FieldRuleUpsertRequest(
            @NotBlank String fieldCode,
            @NotBlank String ruleType,
            @NotBlank String ruleExpression,
            String description
    ) {
    }

    public record SnapshotCreateRequest(
            @NotNull Long versionId,
            @NotBlank String businessType,
            @NotNull Long businessId,
            @NotBlank String dataJson
    ) {
    }

    public record TemplateUpdateRequest(
            @NotBlank String templateName,
            String description
    ) {
    }
}
