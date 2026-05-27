package com.company.oa.auth;

public record LoginResponse(
        String accessToken,
        long expiresIn,
        AuthUser user,
        boolean passwordExpired,
        boolean requires2FA
) {
}
