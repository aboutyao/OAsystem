package com.company.oa.common.service;

public final class OaUtils {
    private OaUtils() {}

    public static Long toLong(Object value) {
        if (value == null) return null;
        return ((Number) value).longValue();
    }

    public static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
