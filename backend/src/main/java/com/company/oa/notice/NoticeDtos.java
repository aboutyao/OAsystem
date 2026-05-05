package com.company.oa.notice;

import jakarta.validation.constraints.NotBlank;

public final class NoticeDtos {
    private NoticeDtos() {
    }

    public record NoticeCreateRequest(
            @NotBlank String title,
            @NotBlank String content,
            String category,
            String publishScopeType,
            int topFlag
    ) {
    }

    public record NoticeUpdateRequest(
            @NotBlank String title,
            @NotBlank String content,
            String category,
            String publishScopeType,
            int topFlag
    ) {
    }
}
