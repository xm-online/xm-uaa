package com.icthh.xm.uaa.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.uaa.security.AuthoritiesConstants.ADMIN;
import static com.icthh.xm.uaa.security.AuthoritiesConstants.ANONYMOUS;
import static com.icthh.xm.uaa.security.AuthoritiesConstants.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

/**
 * Test class for the {@link SecurityUtils} utility class.
 */
class SecurityUtilsUnitTest {

    private static final String LOGIN_ADMIN = "admin";
    private static final String LOGIN_ANONYMOUS = "anonymous";

    @BeforeEach
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(LOGIN_ADMIN, LOGIN_ADMIN));
        SecurityContextHolder.setContext(securityContext);
        Optional<String> login = SecurityUtils.getCurrentUserLogin();
        assertThat(login).contains(LOGIN_ADMIN);
    }

    @Test
    void testGetCurrentUserLoginForOAuth2() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Map<String, Object> claims = new HashMap<>();
        claims.put("groups", USER);
        claims.put("sub", 123);
        claims.put("preferred_username", LOGIN_ADMIN);
        OidcIdToken idToken = new OidcIdToken(ID_TOKEN, Instant.now(), Instant.now().plusSeconds(60), claims);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(USER));
        OidcUser user = new DefaultOidcUser(authorities, idToken);
        var auth2AuthenticationToken = new OAuth2AuthenticationToken(user, authorities, "oidc");
        securityContext.setAuthentication(auth2AuthenticationToken);
        SecurityContextHolder.setContext(securityContext);

        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        assertThat(login).contains(LOGIN_ADMIN);
    }

    @Test
    void testExtractAuthorityFromClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("groups", Arrays.asList(ADMIN, USER));

        List<GrantedAuthority> expectedAuthorities = Arrays.asList(
            new SimpleGrantedAuthority(ADMIN),
            new SimpleGrantedAuthority(USER)
        );

        List<GrantedAuthority> authorities = SecurityUtils.extractAuthorityFromClaims(claims);

        assertThat(authorities).isNotNull().isNotEmpty().hasSize(2).containsAll(expectedAuthorities);
    }

    @Test
    void testExtractAuthorityFromClaims_NamespacedRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityUtils.CLAIMS_NAMESPACE + "roles", Arrays.asList(ADMIN, USER));

        List<GrantedAuthority> expectedAuthorities = Arrays.asList(
            new SimpleGrantedAuthority(ADMIN),
            new SimpleGrantedAuthority(USER)
        );

        List<GrantedAuthority> authorities = SecurityUtils.extractAuthorityFromClaims(claims);

        assertThat(authorities).isNotNull().isNotEmpty().hasSize(2).containsAll(expectedAuthorities);
    }

    @Test
    void testIsAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        var token = new UsernamePasswordAuthenticationToken(LOGIN_ADMIN, LOGIN_ADMIN);
        securityContext.setAuthentication(token);
        SecurityContextHolder.setContext(securityContext);
        boolean isAuthenticated = SecurityUtils.isAuthenticated();
        assertThat(isAuthenticated).isTrue();
    }

    @Test
    void testAnonymousIsNotAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        var authorities = Collections.singletonList(new SimpleGrantedAuthority(ANONYMOUS));
        var token = new UsernamePasswordAuthenticationToken(LOGIN_ANONYMOUS, LOGIN_ANONYMOUS, authorities);
        securityContext.setAuthentication(token);
        SecurityContextHolder.setContext(securityContext);
        boolean isAuthenticated = SecurityUtils.isAuthenticated();
        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void testHasCurrentUserThisAuthority() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        var authorities = Collections.singletonList(new SimpleGrantedAuthority(USER));
        var token = new UsernamePasswordAuthenticationToken(LOGIN_ANONYMOUS, LOGIN_ANONYMOUS, authorities);
        securityContext.setAuthentication(token);
        SecurityContextHolder.setContext(securityContext);

        assertThat(SecurityUtils.hasCurrentUserThisAuthority(USER)).isTrue();
        assertThat(SecurityUtils.hasCurrentUserThisAuthority(ADMIN)).isFalse();
    }

    @Test
    void testHasCurrentUserAnyOfAuthorities() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        var authorities = Collections.singletonList(new SimpleGrantedAuthority(USER));
        var token = new UsernamePasswordAuthenticationToken(LOGIN_ANONYMOUS, LOGIN_ANONYMOUS, authorities);
        securityContext.setAuthentication(token);
        SecurityContextHolder.setContext(securityContext);

        assertThat(SecurityUtils.hasCurrentUserAnyOfAuthorities(USER, ADMIN)).isTrue();
        assertThat(SecurityUtils.hasCurrentUserAnyOfAuthorities(ANONYMOUS, ADMIN)).isFalse();
    }

    @Test
    void testHasCurrentUserNoneOfAuthorities() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        var authorities = Collections.singletonList(new SimpleGrantedAuthority(USER));
        var token = new UsernamePasswordAuthenticationToken(LOGIN_ANONYMOUS, LOGIN_ANONYMOUS, authorities);
        securityContext.setAuthentication(token);
        SecurityContextHolder.setContext(securityContext);

        assertThat(SecurityUtils.hasCurrentUserNoneOfAuthorities(USER, ADMIN)).isFalse();
        assertThat(SecurityUtils.hasCurrentUserNoneOfAuthorities(ANONYMOUS, ADMIN)).isTrue();
    }
}
