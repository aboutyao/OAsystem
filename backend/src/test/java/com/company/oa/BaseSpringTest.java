package com.company.oa;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.service.SequenceService;
import com.company.oa.org.mapper.UserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseSpringTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected AuthService authService;

    @Autowired
    protected SequenceService sequenceService;

    @Autowired
    protected UserMapper userMapper;

    @BeforeEach
    void setupSecurityContext() {
        AuthUser admin = new AuthUser(1L, "admin", "系统管理员", 2L, "总经办",
                List.of("SUPER_ADMIN"), List.of("*"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    protected void loginAsAdmin() {
        setupSecurityContext();
    }

    protected void cleanTable(String table) {
        jdbc.update("DELETE FROM " + table);
    }
}
