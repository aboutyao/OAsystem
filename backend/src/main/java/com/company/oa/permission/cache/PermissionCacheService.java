package com.company.oa.permission.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PermissionCacheService {

    private static final String USER_PREVIEW_KEY = "perm:user:preview:";
    private static final String MENU_TREE_KEY = "perm:menu:tree";
    private static final long CACHE_TTL_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    public PermissionCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserPreview(long userId) {
        return (Map<String, Object>) redisTemplate.opsForValue().get(USER_PREVIEW_KEY + userId);
    }

    public void setUserPreview(long userId, Map<String, Object> preview) {
        redisTemplate.opsForValue().set(USER_PREVIEW_KEY + userId, preview, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void invalidateUser(long userId) {
        redisTemplate.delete(USER_PREVIEW_KEY + userId);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> getMenuTree() {
        return (java.util.List<Map<String, Object>>) redisTemplate.opsForValue().get(MENU_TREE_KEY);
    }

    public void setMenuTree(java.util.List<Map<String, Object>> tree) {
        redisTemplate.opsForValue().set(MENU_TREE_KEY, tree, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void invalidateMenuTree() {
        redisTemplate.delete(MENU_TREE_KEY);
    }

    public void invalidateAll() {
        var userKeys = redisTemplate.keys("perm:user:*");
        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
        redisTemplate.delete(MENU_TREE_KEY);
    }
}