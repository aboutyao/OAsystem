package com.company.oa.message;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class MessageDtos {
    private MessageDtos() {
    }

    public record BatchReadRequest(@NotEmpty List<Long> ids) {
    }
}
