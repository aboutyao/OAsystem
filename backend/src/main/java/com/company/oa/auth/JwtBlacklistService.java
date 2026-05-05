package com.company.oa.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JwtBlacklistService {

    private static final String BLACKLIST_KEY = "jwt:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    public JwtBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToBlacklist(String token, long remainingSeconds) {
        if (remainingSeconds > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY + token,
                    "blacklisted",
                    remainingSeconds,
                    TimeUnit.SECONDS
            );
        }
    }

    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY + token);
        return Boolean.TRUE.equals(exists);
    }
}