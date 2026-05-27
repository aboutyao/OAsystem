package com.company.oa.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;

    public AuthController(AuthService authService, JwtService jwtService, JwtBlacklistService jwtBlacklistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtBlacklistService.addToBlacklist(token);
        }
        return Map.of("success", true);
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.oldPassword(), request.newPassword());
        return Map.of("changed", true);
    }

    @GetMapping("/me")
    public AuthUser me() {
        return authService.currentUser();
    }

    @GetMapping("/password-status")
    public Map<String, Object> passwordStatus() {
        return authService.passwordStatus();
    }

    @GetMapping("/menus")
    public List<Map<String, Object>> menus() {
        return authService.menusForCurrentUser();
    }

    // ========== Two-Factor Authentication ==========

    @PostMapping("/2fa/verify")
    public LoginResponse verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return authService.verifyTwoFactor(request.tempToken(), request.code());
    }

    @PostMapping("/2fa/setup")
    public Map<String, Object> setupTwoFactor() {
        return authService.setupTwoFactor();
    }

    @PostMapping("/2fa/enable")
    public Map<String, Object> enableTwoFactor(@Valid @RequestBody TwoFactorEnableRequest request) {
        authService.enableTwoFactor(request.secret(), request.code());
        return Map.of("enabled", true);
    }

    @PostMapping("/2fa/disable")
    public Map<String, Object> disableTwoFactor(@Valid @RequestBody TwoFactorDisableRequest request) {
        authService.disableTwoFactor(request.code());
        return Map.of("disabled", true);
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password,
            String captchaId,
            String captchaCode
    ) {
    }

    public record TwoFactorVerifyRequest(
            @NotBlank String tempToken,
            @NotBlank String code
    ) {
    }

    public record TwoFactorEnableRequest(
            @NotBlank String secret,
            @NotBlank String code
    ) {
    }

    public record TwoFactorDisableRequest(
            @NotBlank String code
    ) {
    }
}