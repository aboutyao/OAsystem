package com.company.oa.common.web;

import com.company.oa.common.api.ApiResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case ErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.BAD_REQUEST, ErrorCode.VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(ex.getCode(), ex.getMessage(), requestId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN, "无访问权限", requestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(ErrorCode.VALIDATION_FAILED, message, requestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "系统异常，请联系管理员", requestId()));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private String requestId() {
        return MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
    }
}
