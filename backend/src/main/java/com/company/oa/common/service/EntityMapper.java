package com.company.oa.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EntityMapper {
    private final ObjectMapper objectMapper;

    public EntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, LinkedHashMap.class);
    }
}
