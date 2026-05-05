package com.company.oa.oa.seal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class SealDtos {
    private SealDtos() {
    }

    public record SealCreateRequest(
            @NotBlank String sealType,
            @NotBlank String sealName,
            @NotBlank String fileTitle,
            String useReason,
            @NotNull LocalDateTime useAt,
            @NotNull @Min(0) @Max(1) int outFlag
    ) {
    }

    public record SealUpdateRequest(
            @NotBlank String sealType,
            @NotBlank String sealName,
            @NotBlank String fileTitle,
            String useReason,
            @NotNull LocalDateTime useAt,
            @NotNull @Min(0) @Max(1) int outFlag
    ) {
    }
}
