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
            long remainingSeconds = jwtService.getRemainingSeconds(token);
            jwtBlacklistService.addToBlacklist(token, remainingSeconds);
        }
        return Map.of("success", true);
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        authService.changePassword(oldPassword, newPassword);
        return Map.of("changed", true);
    }

    @GetMapping("/me")
    public AuthUser me() {
        return authService.currentUser();
    }

    @GetMapping("/menus")
    public List<Map<String, Object>> menus() {
        return authService.menusForCurrentUser();
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password,
            String captchaId,
            String captchaCode
    ) {
    }
}