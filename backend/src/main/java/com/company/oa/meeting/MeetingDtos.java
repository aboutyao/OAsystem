package com.company.oa.meeting;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class MeetingDtos {
    private MeetingDtos() {
    }

    public record RoomCreateRequest(
            @NotBlank String roomName,
            String location,
            @NotNull @Min(0) Integer capacity,
            String equipment,
            String remark,
            @NotBlank String status
    ) {
    }

    public record RoomUpdateRequest(
            @NotBlank String roomName,
            String location,
            @NotNull @Min(0) Integer capacity,
            String equipment,
            String remark,
            @NotBlank String status
    ) {
    }

    public record BookingCreateRequest(
            @NotNull Long roomId,
            @NotBlank String title,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt,
            @NotNull @Min(0) Integer participantCount
    ) {
    }

    public record BookingCancelRequest(String cancelReason) {
    }
}
