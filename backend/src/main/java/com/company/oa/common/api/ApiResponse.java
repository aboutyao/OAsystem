package com.company.oa.common.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String requestId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>("SUCCESS", "OK", data, requestId, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> failure(String code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId, OffsetDateTime.now());
    }
}
