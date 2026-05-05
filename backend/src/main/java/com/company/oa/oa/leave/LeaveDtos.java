package com.company.oa.oa.leave;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class LeaveDtos {
    private LeaveDtos() {
    }

    public record LeaveCreateRequest(
            @NotBlank String leaveType,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            @NotNull BigDecimal durationHours,
            @NotNull BigDecimal durationDays,
            String reason,
            String handoverNote
    ) {
    }

    public record LeaveUpdateRequest(
            @NotBlank String leaveType,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            @NotNull BigDecimal durationHours,
            @NotNull BigDecimal durationDays,
            String reason,
            String handoverNote
    ) {
    }
}
