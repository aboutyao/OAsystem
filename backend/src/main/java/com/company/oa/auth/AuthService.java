package com.company.oa.auth;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.mapper.AuthSqlMapper;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.org.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private static final String ENABLED = "ENABLED";
    private final UserMapper userMapper;
    private final AuthSqlMapper authSqlMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final LoginRateLimiter loginRateLimiter;
    private final TwoFactorService twoFactorService;

    public AuthService(
            UserMapper userMapper,
            AuthSqlMapper authSqlMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            ObjectProvider<HttpServletRequest> requestProvider,
            LoginRateLimiter loginRateLimiter,
            TwoFactorService twoFactorService
    ) {
        this.userMapper = userMapper;
        this.authSqlMapper = authSqlMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.requestProvider = requestProvider;
        this.loginRateLimiter = loginRateLimiter;
        this.twoFactorService = twoFactorService;
    }

    public LoginResponse login(AuthController.LoginRequest request) {
        String ip = clientIp();
        String ua = userAgent();

        if (ip != null && loginRateLimiter.isBlocked(ip)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录尝试次数过多，请稍后再试");
        }

        Map<String, Object> userRow = authSqlMapper.selectUserByUsername(request.username());
        if (userRow == null) {
            safeRecordLoginFailure(request.username(), "账号或密码错误", ip, ua);
            loginRateLimiter.recordFailure(ip);
            throw new BusinessException(ErrorCode.USER_BAD_CREDENTIALS, "账号或密码错误");
        }

        Long userId = ((Number) userRow.get("id")).longValue();
        try {
            assertLoginAllowed(userRow);
        } catch (BusinessException ex) {
            safeRecordLoginFailure(request.username(), ex.getMessage(), ip, ua);
            loginRateLimiter.recordFailure(ip);
            throw ex;
        }

        String storedHash = String.valueOf(userRow.get("password_hash"));
        if (!passwordEncoder.matches(request.password(), storedHash)) {
            recordLoginFailure(userId);
            safeRecordLoginFailure(request.username(), "账号或密码错误", ip, ua);
            loginRateLimiter.recordFailure(ip);
            throw new BusinessException(ErrorCode.USER_BAD_CREDENTIALS, "账号或密码错误");
        }

        userMapper.updateLoginSuccess(userId);
        loginRateLimiter.reset(ip);

        boolean passwordExpired = isPasswordExpired(userRow);

        // Check if 2FA is enabled
        Map<String, Object> totpInfo = authSqlMapper.selectTotpInfo(userId);
        boolean totpEnabled = totpInfo != null && Boolean.TRUE.equals(totpInfo.get("totp_enabled"));

        AuthUser user = loadUser(userId);
        safeRecordLoginSuccess(userId, user.username(), ip, ua);

        if (totpEnabled) {
            // Return a temporary token and flag that 2FA is required
            String tempToken = jwtService.generateTempToken(userId);
            return new LoginResponse(tempToken, 300, user, passwordExpired, true);
        }

        return new LoginResponse(jwtService.generateToken(user), jwtService.expiresInSeconds(), user, passwordExpired, false);
    }

    private void safeRecordLoginSuccess(Long userId, String username, String ip, String ua) {
        try {
            auditService.recordLoginSuccess(userId, username, ip, ua);
        } catch (Exception ignored) {
        }
    }

    private void safeRecordLoginFailure(String username, String reason, String ip, String ua) {
        try {
            auditService.recordLoginFailure(username, reason, ip, ua);
        } catch (Exception ignored) {
        }
    }

    private String clientIp() {
        HttpServletRequest req = requestProvider.getIfAvailable();
        if (req == null) {
            return null;
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return req.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest req = requestProvider.getIfAvailable();
        if (req == null) {
            return null;
        }
        return req.getHeader("User-Agent");
    }

    public AuthUser loadUser(Long userId) {
        Map<String, Object> row = authSqlMapper.selectUserWithDept(userId);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        List<String> roles = authSqlMapper.selectUserRoles(userId);
        List<String> permissions = loadPermissions(userId, roles);

        return new AuthUser(
                ((Number) row.get("id")).longValue(),
                String.valueOf(row.get("username")),
                String.valueOf(row.get("real_name")),
                row.get("main_dept_id") == null ? null : ((Number) row.get("main_dept_id")).longValue(),
                row.get("main_dept_name") == null ? null : String.valueOf(row.get("main_dept_name")),
                roles,
                permissions
        );
    }

    public AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return authUser;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> menusForCurrentUser() {
        AuthUser user = currentUser();
        if (user.permissions().contains("*")) {
            return authSqlMapper.selectAllVisibleMenus();
        }
        return authSqlMapper.selectMenusByUserId(user.id());
    }

    private void assertLoginAllowed(Map<String, Object> userRow) {
        if ("RESIGNED".equals(String.valueOf(userRow.get("employee_status")))) {
            throw new BusinessException(ErrorCode.USER_BAD_CREDENTIALS, "账号或密码错误");
        }
        if (!ENABLED.equals(String.valueOf(userRow.get("account_status")))) {
            throw new BusinessException(ErrorCode.USER_BAD_CREDENTIALS, "账号或密码错误");
        }
        Object lockedUntil = userRow.get("locked_until");
        if (lockedUntil instanceof Timestamp timestamp && timestamp.toInstant().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.USER_ACCOUNT_LOCKED, "账号已锁定，请稍后再试");
        }
    }

    private void recordLoginFailure(Long userId) {
        int maxFailCount = intConfig("security.login.maxFailCount", 5);
        int lockMinutes = intConfig("security.login.lockMinutes", 15);
        userMapper.updateLoginFailCount(userId);
        Integer failCount = authSqlMapper.selectLoginFailCount(userId);
        int count = failCount == null ? 0 : failCount;
        if (count >= maxFailCount) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
            userMapper.updateLockedUntil(userId, lockedUntil);
        }
    }

    private int intConfig(String key, int defaultValue) {
        String value = authSqlMapper.selectConfigValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private List<String> loadPermissions(Long userId, List<String> roles) {
        if (roles.contains("SUPER_ADMIN")) {
            return List.of("*");
        }
        List<String> permissions = new ArrayList<>(authSqlMapper.selectUserButtonPermissions(userId));
        permissions.addAll(authSqlMapper.selectUserMenuCodes(userId));
        return permissions.stream().distinct().toList();
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        AuthUser user = currentUser();
        Long userId = user.id();

        String currentHash = authSqlMapper.selectPasswordHash(userId);
        if (currentHash == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, currentHash)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "原密码不正确");
        }

        validatePasswordComplexity(newPassword);

        LocalDateTime now = LocalDateTime.now();
        int expiryDays = intConfig("security.password.expiryDays", 90);
        LocalDateTime expiresAt = now.plusDays(expiryDays);
        userMapper.updatePasswordHash(userId, passwordEncoder.encode(newPassword), now, expiresAt);
    }

    private void validatePasswordComplexity(String password) {
        int minLength = intConfig("security.password.minLength", 8);
        int maxLength = intConfig("security.password.maxLength", 64);
        if (password == null || password.length() < minLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度不能少于" + minLength + "位");
        }
        if (password.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度不能超过" + maxLength + "位");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含至少一个大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含至少一个小写字母");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含至少一个数字");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含至少一个特殊字符");
        }
    }

    private boolean isPasswordExpired(Map<String, Object> userRow) {
        Object expiresAt = userRow.get("password_expires_at");
        if (expiresAt == null) {
            return false;
        }
        LocalDateTime expiryTime;
        if (expiresAt instanceof Timestamp ts) {
            expiryTime = ts.toLocalDateTime();
        } else if (expiresAt instanceof LocalDateTime ldt) {
            expiryTime = ldt;
        } else {
            return false;
        }
        return expiryTime.isBefore(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> passwordStatus() {
        AuthUser user = currentUser();
        Map<String, Object> row = authSqlMapper.selectUserByUsername(user.username());
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        Object expiresAt = row.get("password_expires_at");
        if (expiresAt == null) {
            result.put("expired", false);
            result.put("expiresAt", null);
            result.put("daysRemaining", null);
            return result;
        }

        LocalDateTime expiryTime;
        if (expiresAt instanceof Timestamp ts) {
            expiryTime = ts.toLocalDateTime();
        } else if (expiresAt instanceof LocalDateTime ldt) {
            expiryTime = ldt;
        } else {
            result.put("expired", false);
            result.put("expiresAt", null);
            result.put("daysRemaining", null);
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean expired = expiryTime.isBefore(now);
        long daysRemaining = java.time.Duration.between(now, expiryTime).toDays();

        result.put("expired", expired);
        result.put("expiresAt", expiryTime);
        result.put("daysRemaining", daysRemaining);
        return result;
    }

    // ========== Two-Factor Authentication ==========

    @Transactional
    public LoginResponse verifyTwoFactor(String tempToken, String code) {
        if (!jwtService.isTempToken(tempToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的临时令牌");
        }

        Long userId = jwtService.parseUserId(tempToken);
        Map<String, Object> totpInfo = authSqlMapper.selectTotpInfo(userId);
        if (totpInfo == null || !Boolean.TRUE.equals(totpInfo.get("totp_enabled"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "二步验证未启用");
        }

        String secret = String.valueOf(totpInfo.get("totp_secret"));
        if (!twoFactorService.verifyCode(secret, code)) {
            throw new BusinessException(ErrorCode.USER_BAD_CREDENTIALS, "验证码错误");
        }

        AuthUser user = loadUser(userId);
        Map<String, Object> userRow = authSqlMapper.selectUserByUsername(user.username());
        boolean passwordExpired = isPasswordExpired(userRow);

        return new LoginResponse(jwtService.generateToken(user), jwtService.expiresInSeconds(), user, passwordExpired, false);
    }

    @Transactional
    public Map<String, Object> setupTwoFactor() {
        AuthUser authUser = currentUser();
        String secret = twoFactorService.generateSecret();
        String qrCodeImage = twoFactorService.getQrCodeImage(secret, authUser.username(), "OA System");

        Map<String, Object> result = new HashMap<>();
        result.put("secret", secret);
        result.put("qrCode", qrCodeImage);
        return result;
    }

    @Transactional
    public void enableTwoFactor(String secret, String code) {
        AuthUser authUser = currentUser();

        if (!twoFactorService.verifyCode(secret, code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误，请重试");
        }

        userMapper.enableTotp(authUser.id(), secret);
    }

    @Transactional
    public void disableTwoFactor(String code) {
        AuthUser authUser = currentUser();
        Map<String, Object> totpInfo = authSqlMapper.selectTotpInfo(authUser.id());

        if (totpInfo == null || !Boolean.TRUE.equals(totpInfo.get("totp_enabled"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "二步验证未启用");
        }

        String secret = String.valueOf(totpInfo.get("totp_secret"));
        if (!twoFactorService.verifyCode(secret, code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
        }

        userMapper.disableTotp(authUser.id());
    }
}