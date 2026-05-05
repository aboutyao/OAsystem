package com.company.oa.auth;

import java.util.List;

public record AuthUser(
        Long id,
        String username,
        String realName,
        Long mainDeptId,
        String mainDeptName,
        List<String> roles,
        List<String> permissions
) {
}
