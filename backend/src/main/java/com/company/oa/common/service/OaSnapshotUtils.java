package com.company.oa.common.service;

import com.company.oa.org.mapper.UserMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OaSnapshotUtils {

    private OaSnapshotUtils() {
    }

    public static Map<String, String> loadUserDeptSnapshot(long userId, UserMapper userMapper) {
        Map<String, Object> r = userMapper.selectUserDeptSnapshot(userId);
        if (r == null) {
            return Map.of("deptId", "", "deptName", "");
        }
        Map<String, String> m = new LinkedHashMap<>();
        m.put("deptName", r.get("deptName") == null ? "" : String.valueOf(r.get("deptName")));
        m.put("deptId", r.get("deptId") != null
                ? String.valueOf(((Number) r.get("deptId")).longValue()) : "");
        return m;
    }
}
