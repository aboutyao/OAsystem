package com.company.oa.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * Shared utility for converting JPA/MyBatis entities to Map<String, Object>.
 * Replaces duplicated toMap() methods across service classes.
 */
public final class OaEntityMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private OaEntityMapper() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object entity) {
        return MAPPER.convertValue(entity, Map.class);
    }
}
