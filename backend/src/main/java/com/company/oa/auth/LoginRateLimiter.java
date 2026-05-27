package com.company.oa.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LoginRateLimiter {
    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "login:rate:";
    private static final int MAX_ATTEMPTS = 10;
    private static final int WINDOW_MINUTES = 5;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isBlocked(String ip) {
        String key = KEY_PREFIX + ip;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) return false;
        return Integer.parseInt(val) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW_MINUTES, TimeUnit.MINUTES);
        }
    }

    public void reset(String ip) {
        redisTemplate.delete(KEY_PREFIX + ip);
    }
}
