package com.company.oa.auth;

import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expiresInSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${security.jwt.secret:}") String secret,
            @Value("${security.jwt.expires-in-seconds:7200}") long expiresInSeconds
    ) {
        if (secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 characters. Set it in application.yml or JWT_SECRET env var.");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expiresInSeconds = expiresInSeconds;
    }

    public String generateToken(AuthUser user) {
        long now = Instant.now().getEpochSecond();
        return sign(
                Map.of("alg", "HS256", "typ", "JWT"),
                Map.of(
                        "sub", String.valueOf(user.id()),
                        "username", user.username(),
                        "iat", now,
                        "exp", now + expiresInSeconds
                )
        );
    }

    public String generateTempToken(Long userId) {
        long now = Instant.now().getEpochSecond();
        return sign(
                Map.of("alg", "HS256", "typ", "JWT"),
                Map.of(
                        "sub", String.valueOf(userId),
                        "type", "2fa_temp",
                        "iat", now,
                        "exp", now + 300
                )
        );
    }

    public boolean isTempToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            Map<?, ?> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            return "2fa_temp".equals(payload.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public Long parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token");
            }
            String unsigned = parts[0] + "." + parts[1];
            String expectedSignature = base64Url(hmac(unsigned));
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Invalid signature");
            }
            Map<?, ?> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("Token expired");
            }
            return Long.valueOf(String.valueOf(payload.get("sub")));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态无效或已过期");
        }
    }

    public long expiresInSeconds() {
        return expiresInSeconds;
    }

    private String sign(Map<String, Object> header, Map<String, Object> payload) {
        try {
            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String unsigned = encodedHeader + "." + encodedPayload;
            return unsigned + "." + base64Url(hmac(unsigned));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate token", ex);
        }
    }

    private byte[] hmac(String input) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret, HMAC_SHA256));
        return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigestUtil.constantTimeEquals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MessageDigestUtil {
        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            if (a.length != b.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
