package com.company.oa.common.service;

import com.company.oa.system.mapper.SysConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class ConfigService {
    private final SysConfigMapper sysConfigMapper;

    public ConfigService(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    public String getString(String key) {
        return sysConfigMapper.selectValueByKey(key);
    }

    public String getString(String key, String defaultValue) {
        String v = sysConfigMapper.selectValueByKey(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    public int getInt(String key, int defaultValue) {
        String v = sysConfigMapper.selectValueByKey(key);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String v = sysConfigMapper.selectValueByKey(key);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
