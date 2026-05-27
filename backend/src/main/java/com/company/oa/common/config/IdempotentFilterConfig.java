package com.company.oa.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdempotentFilterConfig {
    @Bean
    public FilterRegistrationBean<IdempotentFilter> idempotentFilter(IdempotentFilter filter) {
        FilterRegistrationBean<IdempotentFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
