package com.company.oa;

import com.company.oa.audit.AuditService;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.auth.AuthController;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.auth.JwtService;
import com.company.oa.auth.mapper.AuthSqlMapper;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest extends BaseMySqlTest {

    @Test
    void loginUsesSeededAdminAndReturnsJwtWithPermissions() {
        // Clean up audit_login_log to prevent pollution from other tests
        jdbc.update("DELETE FROM audit_login_log");

        JwtService jwtService = new JwtService(new ObjectMapper(), "test-secret-must-be-at-least-32-characters-long", 7200);

        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        AuditService auditService = new AuditService(
                getMapper(AuditLoginLogMapper.class),
                getMapper(AuditOperationLogMapper.class),
                getMapper(SysConfigMapper.class),
                sequenceService
        );
        ObjectProvider<HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        when(requestProvider.getIfAvailable()).thenReturn(null);
        AuthService authService = new AuthService(
                getMapper(UserMapper.class),
                getMapper(AuthSqlMapper.class),
                PasswordEncoderFactories.createDelegatingPasswordEncoder(),
                jwtService,
                auditService,
                requestProvider
        );

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
        JwtService jwtService = new JwtService(new ObjectMapper(), "test-secret-must-be-at-least-32-characters-long", 7200);

        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        AuditService auditService = new AuditService(
                getMapper(AuditLoginLogMapper.class),
                getMapper(AuditOperationLogMapper.class),
                getMapper(SysConfigMapper.class),
                sequenceService
        );
        ObjectProvider<HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        when(requestProvider.getIfAvailable()).thenReturn(null);
        AuthService authService = new AuthService(
                getMapper(UserMapper.class),
                getMapper(AuthSqlMapper.class),
                PasswordEncoderFactories.createDelegatingPasswordEncoder(),
                jwtService,
                auditService,
                requestProvider
        );

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
