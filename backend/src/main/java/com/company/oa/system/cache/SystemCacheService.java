package com.company.oa.system.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SystemCacheService {

    private static final String CONFIG_KEY = "sys:config:";
    private static final String DICT_KEY = "sys:dict:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    public SystemCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getConfigValue(String key) {
        return (String) redisTemplate.opsForValue().get(CONFIG_KEY + key);
    }

    public void setConfigValue(String key, String value) {
        redisTemplate.opsForValue().set(CONFIG_KEY + key, value, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDictItems(String dictType) {
        return (List<Map<String, Object>>) redisTemplate.opsForValue().get(DICT_KEY + dictType);
    }

    public void setDictItems(String dictType, List<Map<String, Object>> items) {
        redisTemplate.opsForValue().set(DICT_KEY + dictType, items, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void invalidateConfig(String key) {
        redisTemplate.delete(CONFIG_KEY + key);
    }

    public void invalidateDict(String dictType) {
        redisTemplate.delete(DICT_KEY + dictType);
    }

    public void invalidateAll() {
        deleteByPattern("sys:config:*");
        deleteByPattern("sys:dict:*");
    }

    private void deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}