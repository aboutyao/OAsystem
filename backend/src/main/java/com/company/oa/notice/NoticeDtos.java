package com.company.oa.notice;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class NoticeDtos {
    private NoticeDtos() {
    }

    public record NoticeCreateRequest(
            @NotBlank String title,
            @NotBlank String content,
            String category,
            String publishScopeType,
            int topFlag,
            LocalDateTime scheduledAt
    ) {
    }

    public record NoticeUpdateRequest(
            @NotBlank String title,
            @NotBlank String content,
            String category,
            String publishScopeType,
            int topFlag,
            LocalDateTime scheduledAt
    ) {
    }
}
