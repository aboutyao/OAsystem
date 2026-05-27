package com.company.oa.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotentFilter implements Filter {
    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "idempotent:";
    private static final long EXPIRY_SECONDS = 5;

    public IdempotentFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Only apply to POST/PUT/DELETE
        String method = httpReq.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Skip idempotency for file uploads, login, and 2FA
        String path = httpReq.getRequestURI();
        if (path.contains("/upload") || path.contains("/auth/login") ||
            path.contains("/auth/2fa") || path.contains("/files/")) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = httpReq.getHeader("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String key = PREFIX + idempotencyKey;
        Boolean firstRequest = redisTemplate.opsForValue().setIfAbsent(key, "1", EXPIRY_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(firstRequest)) {
            httpResp.setStatus(409);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write("{\"code\":\"DUPLICATE_REQUEST\",\"message\":\"重复请求，请勿重复提交\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
