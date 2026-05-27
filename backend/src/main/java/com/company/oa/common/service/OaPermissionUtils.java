package com.company.oa.common.service;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import java.util.Map;

public final class OaPermissionUtils {
    private OaPermissionUtils() {}

    public static void assertOwner(Map<String, Object> row, AuthService authService, String entityDesc) {
        AuthUser user = authService.currentUser();
        if (user.permissions().contains("*")) return;
        Object v = row.get("createdBy");
        long owner = v == null ? -1 : ((Number) v).longValue();
        if (!user.id().equals(owner)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作" + entityDesc);
        }
    }

    public static void assertViewAllowed(Map<String, Object> row, AuthService authService, String entityDesc) {
        assertOwner(row, authService, entityDesc);
    }
}
