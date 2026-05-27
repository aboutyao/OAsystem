package com.company.oa.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
public class JwtBlacklistService {

    private static final String BLACKLIST_KEY = "jwt:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final long expiresInSeconds;

    public JwtBlacklistService(RedisTemplate<String, Object> redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.expiresInSeconds = jwtService.expiresInSeconds();
    }

    public void addToBlacklist(String token) {
        if (expiresInSeconds > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY + sha256Hex(token),
                    "blacklisted",
                    expiresInSeconds,
                    TimeUnit.SECONDS
            );
        }
    }

    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY + sha256Hex(token));
        return Boolean.TRUE.equals(exists);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}