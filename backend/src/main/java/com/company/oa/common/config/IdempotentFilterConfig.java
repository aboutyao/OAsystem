package com.company.oa.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class IdempotentFilterConfig {
    @Bean
    public IdempotentFilter idempotentFilter(StringRedisTemplate redisTemplate) {
        return new IdempotentFilter(redisTemplate);
    }

    @Bean
    public FilterRegistrationBean<IdempotentFilter> idempotentFilterRegistration(IdempotentFilter filter) {
        FilterRegistrationBean<IdempotentFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
