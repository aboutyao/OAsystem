package com.company.oa.common.filter;

import com.company.oa.common.service.ConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Global per-IP rate limiting filter backed by Redis sliding window counters.
 * <p>
 * Each client IP gets a fixed quota (default 100) per 60-second window.
 * The quota is configurable via the system config key {@code rate-limit.max-requests-per-minute}.
 * <p>
 * Health checks, static assets, and the actuator endpoints are excluded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "ratelimit:";
    private static final long WINDOW_SECONDS = 60;

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/api/ops/health",
            "/actuator/health",
            "/actuator/",
            "/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/ws/"
    );

    private static final Set<String> SKIP_EXTENSIONS = Set.of(
            ".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".ico",
            ".svg", ".woff", ".woff2", ".ttf", ".eot", ".map"
    );

    private static final int DEFAULT_MAX_REQUESTS = 100;

    private final StringRedisTemplate redisTemplate;
    private final ConfigService configService;

    public RateLimitFilter(StringRedisTemplate redisTemplate, ConfigService configService) {
        this.redisTemplate = redisTemplate;
        this.configService = configService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (shouldSkip(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        int maxRequests = configService.getInt("rate-limit.max-requests-per-minute", DEFAULT_MAX_REQUESTS);

        String key = KEY_PREFIX + clientIp + ":" + (System.currentTimeMillis() / 1000 / WINDOW_SECONDS);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS + 10, java.util.concurrent.TimeUnit.SECONDS);
        }

        long limit = maxRequests;
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - (count == null ? 0 : count))));

        if (count != null && count > maxRequests) {
            long retryAfter = WINDOW_SECONDS - (System.currentTimeMillis() / 1000 % WINDOW_SECONDS) + 1;
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁，请稍后再试\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String uri) {
        for (String prefix : SKIP_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        for (String ext : SKIP_EXTENSIONS) {
            if (uri.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
