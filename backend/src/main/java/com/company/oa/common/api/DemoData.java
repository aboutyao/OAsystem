package com.company.oa.common.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DemoData {
    private DemoData() {
    }

    public static Map<String, Object> map(Object... entries) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            data.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return data;
    }

    public static PageResponse<Map<String, Object>> page(long page, long size, List<Map<String, Object>> items) {
        return new PageResponse<>(page, size, items.size(), items);
    }

    public static List<Map<String, Object>> list(Map<String, Object>... items) {
        return new ArrayList<>(List.of(items));
    }
}
