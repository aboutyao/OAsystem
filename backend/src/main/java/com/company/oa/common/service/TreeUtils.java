package com.company.oa.common.service;

import java.util.*;
import java.util.function.Function;

public final class TreeUtils {

    private TreeUtils() {}

    public static <T, K> List<Map<String, Object>> build(
            List<T> items,
            Function<T, K> idGetter,
            Function<T, K> parentIdGetter,
            Function<T, String> labelGetter,
            Function<T, String> iconGetter
    ) {
        Map<K, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (T item : items) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", idGetter.apply(item));
            node.put("label", labelGetter.apply(item));
            node.put("icon", iconGetter != null ? iconGetter.apply(item) : null);
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(idGetter.apply(item), node);
        }

        for (T item : items) {
            K id = idGetter.apply(item);
            K parentId = parentIdGetter.apply(item);
            Map<String, Object> node = nodeMap.get(id);
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(node);
            }
        }

        return roots;
    }

    public static <T, K> List<Map<String, Object>> buildFlat(
            List<T> items,
            Function<T, K> idGetter,
            Function<T, K> parentIdGetter,
            Function<T, String> labelGetter
    ) {
        return build(items, idGetter, parentIdGetter, labelGetter, null);
    }
}
