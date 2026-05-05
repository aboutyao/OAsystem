package com.company.oa.auth;

import com.company.oa.common.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthService authService;
    private final JwtBlacklistService blacklistService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthService authService, JwtBlacklistService blacklistService) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            try {
                if (blacklistService.isBlacklisted(token)) {
                    SecurityContextHolder.clearContext();
                } else {
                    Long userId = jwtService.parseUserId(token);
                    AuthUser user = authService.loadUser(userId);
                    var authorities = user.permissions()
                            .stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null, authorities)
                    );
                }
            } catch (BusinessException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
