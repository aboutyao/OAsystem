package com.company.oa.common.service;

import org.springframework.stereotype.Component;

@Component
public class PageUtils {
    private final ConfigService configService;

    public PageUtils(ConfigService configService) {
        this.configService = configService;
    }

    public long[] clamp(long page, long size) {
        int def = configService.getInt("paging.defaultSize", 20);
        int max = configService.getInt("paging.maxSize", 100);
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? def : size;
        if (s > max) s = max;
        return new long[]{p, s};
    }
}
