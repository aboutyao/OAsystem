package com.company.oa;

import com.company.oa.auth.AuthController;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.auth.JwtService;
import com.company.oa.common.service.SequenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest extends BaseSpringTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SequenceService sequenceService;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM audit_login_log");
    }

    @Test
    void loginUsesSeededAdminAndReturnsJwtWithPermissions() {
        var response = authService.login(new AuthController.LoginRequest("admin", "admin123", null, null));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(7200);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().roles()).contains("SUPER_ADMIN");
        assertThat(response.user().permissions()).contains("*");
        assertThat(jwtService.parseUserId(response.accessToken())).isEqualTo(1L);

        Long n = jdbc.queryForObject(
                "select count(*) from audit_login_log where username = 'admin' and login_result = 'SUCCESS'",
                Long.class);
        assertThat(n).isEqualTo(1L);
    }

    @Test
    void menusForSuperAdminIncludesSeededOaMenus() {
        AuthUser user = authService.loadUser(1L);
        var authorities = user.permissions().stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, authorities)
        );
        try {
            List<Map<String, Object>> menus = authService.menusForCurrentUser();
            assertThat(menus).isNotEmpty();
            List<String> codes = menus.stream().map(m -> String.valueOf(m.get("menuCode"))).collect(Collectors.toList());
            assertThat(codes).contains("dashboard", "contracts", "oa_leaves", "oa_expenses", "oa_seals", "oa_purchases", "notices", "applications");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
