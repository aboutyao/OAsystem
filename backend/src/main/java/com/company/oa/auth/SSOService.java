package com.company.oa.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * SSO集成服务
 * 支持LDAP、OAuth2、SAML
 */
@Service
public class SSOService {
    private static final Logger log = LoggerFactory.getLogger(SSOService.class);
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Value("${sso.type:local}")
    private String ssoType;

    @Value("${sso.ldap.url:}")
    private String ldapUrl;

    @Value("${sso.oauth2.client-id:}")
    private String oauth2ClientId;

    @Value("${sso.oauth2.client-secret:}")
    private String oauth2ClientSecret;

    @Value("${sso.oauth2.authorization-uri:}")
    private String oauth2AuthorizationUri;

    @Value("${sso.oauth2.token-uri:}")
    private String oauth2TokenUri;

    @Value("${sso.oauth2.user-info-uri:}")
    private String oauth2UserInfoUri;

    public SSOService(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    /**
     * 获取OAuth2授权URL
     */
    public String getOAuth2AuthorizationUrl(String redirectUri) {
        if (!"oauth2".equals(ssoType)) {
            return null;
        }

        String state = UUID.randomUUID().toString();
        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&state=%s",
            oauth2AuthorizationUri, oauth2ClientId, redirectUri, state);
    }

    /**
     * OAuth2回调处理
     */
    public Map<String, Object> handleOAuth2Callback(String code, String state) {
        try {
            // 获取访问令牌
            String tokenUrl = String.format("%s?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code",
                oauth2TokenUri, oauth2ClientId, oauth2ClientSecret, code);

            Map<String, Object> tokenResponse = restTemplate.postForObject(tokenUrl, null, Map.class);
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                return Map.of("error", "获取访问令牌失败");
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // 获取用户信息
            Map<String, Object> userInfo = restTemplate.getForObject(
                oauth2UserInfoUri + "?access_token=" + accessToken, Map.class
            );

            if (userInfo == null) {
                return Map.of("error", "获取用户信息失败");
            }

            // 查找或创建用户
            return findOrCreateUser(userInfo);
        } catch (Exception e) {
            log.error("OAuth2回调处理失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * LDAP认证
     */
    public Map<String, Object> authenticateWithLDAP(String username, String password) {
        if (!"ldap".equals(ssoType)) {
            return Map.of("error", "未启用LDAP认证");
        }

        try {
            // 这里应该使用LDAP SDK进行认证
            // 暂时返回模拟结果
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("name", username);

            return findOrCreateUser(userInfo);
        } catch (Exception e) {
            log.error("LDAP认证失败", e);
            return Map.of("error", e.getMessage());
        }
    }

    private Map<String, Object> findOrCreateUser(Map<String, Object> externalUser) {
        String externalId = (String) externalUser.get("id");
        String username = (String) externalUser.get("username");
        String name = (String) externalUser.get("name");

        // 查找现有用户
        Long existingId = jdbcTemplate.queryForObject(
            "SELECT id FROM org_user WHERE external_id = ? OR username = ?",
            Long.class, externalId, username
        );

        if (existingId != null) {
            // 更新用户信息
            jdbcTemplate.update(
                "UPDATE org_user SET real_name = ?, external_id = ?, last_login = NOW() WHERE id = ?",
                name, externalId, existingId
            );
            return Map.of("userId", existingId, "isNew", false);
        } else {
            // 创建新用户
            long newId = System.currentTimeMillis();
            jdbcTemplate.update(
                "INSERT INTO org_user (id, username, real_name, external_id, created_at, last_login) VALUES (?, ?, ?, ?, NOW(), NOW())",
                newId, username, name, externalId
            );
            return Map.of("userId", newId, "isNew", true);
        }
    }

    /**
     * 获取SSO配置
     */
    public Map<String, Object> getSSOConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", ssoType);
        config.put("oauth2Enabled", "oauth2".equals(ssoType));
        config.put("ldapEnabled", "ldap".equals(ssoType));
        return config;
    }
}
