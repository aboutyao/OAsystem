package com.company.oa.system;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class SystemDtos {
    private SystemDtos() {
    }

    public record ConfigUpdateRequest(@NotBlank String configValue) {
    }

    public record NumberRuleCreateRequest(
            @NotBlank String ruleCode,
            @NotBlank String businessType,
            @NotBlank String prefix,
            String datePattern,
            @Min(1) int seqLength,
            @NotBlank String seqReset,
            String description
    ) {
    }

    public record WorkCalendarUpsertRequest(
            @NotNull LocalDate calDate,
            @NotBlank String dayType,
            String description
    ) {
    }

    public record ImportTaskCreateRequest(
            @NotBlank String businessType,
            String fileName,
            Long fileSize,
            Integer totalRows,
            Integer successRows,
            Integer failedRows,
            String status,
            String errorSummary
    ) {
    }

    public record ExportTaskCreateRequest(
            @NotBlank String businessType,
            String filterJson,
            String fileName,
            Long fileSize,
            Integer rowCount,
            String status,
            String errorSummary
    ) {
    }

    public record DictTypeCreateRequest(
            @NotBlank String dictCode,
            @NotBlank String dictName,
            String remark
    ) {
    }

    public record DictTypeUpdateRequest(
            @NotBlank String dictName,
            String remark
    ) {
    }

    public record DictItemCreateRequest(
            @NotNull Long dictTypeId,
            @NotBlank String itemLabel,
            @NotBlank String itemValue,
            int sortOrder
    ) {
    }

    public record DictItemUpdateRequest(
            @NotBlank String itemLabel,
            @NotBlank String itemValue,
            int sortOrder
    ) {
    }
}
