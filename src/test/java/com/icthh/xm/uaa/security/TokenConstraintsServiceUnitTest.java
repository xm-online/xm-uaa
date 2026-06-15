package com.icthh.xm.uaa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for per-client token lifetime resolution in {@link TokenConstraintsService}.
 *
 * <p>Naming convention: test method describes the scenario and expected outcome.
 */
@RunWith(MockitoJUnitRunner.class)
public class TokenConstraintsServiceUnitTest {

    /** 24 h — mirrors the typical tenant-level default set in external uaa.yaml */
    private static final int GLOBAL_ACCESS_SECONDS = 86_400;
    /** 90 d — mirrors the typical tenant-level default set in external uaa.yaml */
    private static final int GLOBAL_REFRESH_SECONDS = 7_776_000;

    private static final String CLIENT_A = "client-a";
    private static final String CLIENT_B = "client-b";
    private static final String UNKNOWN_CLIENT = "unknown-client";

    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ClientDetailsService clientDetailsService;

    private TokenConstraintsService service;

    private TenantProperties.Security security;

    @Before
    public void setUp() {
        service = new TokenConstraintsService(tenantPropertiesService, applicationProperties, clientDetailsService);

        security = new TenantProperties.Security();
        security.setAccessTokenValiditySeconds(GLOBAL_ACCESS_SECONDS);
        security.setRefreshTokenValiditySeconds(GLOBAL_REFRESH_SECONDS);

        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setSecurity(security);

        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        ApplicationProperties.Security appSecurity = new ApplicationProperties.Security();
        when(applicationProperties.getSecurity()).thenReturn(appSecurity);
    }

    // -----------------------------------------------------------------------
    // Tests: no client-specific config → defaults are used
    // -----------------------------------------------------------------------

    @Test
    public void givenNoClientConfig_whenResolveAccess_thenReturnsGlobalDefault() {
        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenNoClientConfig_whenResolveRefresh_thenReturnsGlobalDefault() {
        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenNullClientId_whenResolveAccess_thenReturnsEmpty() {
        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(null);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenNullClientId_whenResolveRefresh_thenReturnsEmpty() {
        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(null);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenUnknownClient_whenResolveAccess_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, 1800, null);

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(UNKNOWN_CLIENT);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenUnknownClient_whenResolveRefresh_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, null, 3600);

        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(UNKNOWN_CLIENT);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Tests: client-specific access token config
    // -----------------------------------------------------------------------

    @Test
    public void givenClientWithAccessConfig_whenResolveAccess_thenReturnsClientValue() {
        int reducedAccess = 1800; // 30 minutes
        addClientConfig(CLIENT_A, reducedAccess, null);

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).hasValue(reducedAccess);
    }

    @Test
    public void givenClientWithAccessConfigOnly_whenResolveRefresh_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, 1800, null); // only access configured

        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Tests: client-specific refresh token config
    // -----------------------------------------------------------------------

    @Test
    public void givenClientWithRefreshConfig_whenResolveRefresh_thenReturnsClientValue() {
        int reducedRefresh = 43200; // 12 hours
        addClientConfig(CLIENT_A, null, reducedRefresh);

        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(CLIENT_A);

        assertThat(result).hasValue(reducedRefresh);
    }

    @Test
    public void givenClientWithRefreshConfigOnly_whenResolveAccess_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, null, 43200); // only refresh configured

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Tests: both values configured
    // -----------------------------------------------------------------------

    @Test
    public void givenClientWithBothConfigs_whenResolve_thenBothReturnClientValues() {
        int reducedAccess = 900;
        int reducedRefresh = 28800;
        addClientConfig(CLIENT_A, reducedAccess, reducedRefresh);

        assertThat(service.resolveClientSpecificAccessTokenValidity(CLIENT_A)).hasValue(reducedAccess);
        assertThat(service.resolveClientSpecificRefreshTokenValidity(CLIENT_A)).hasValue(reducedRefresh);
    }

    // -----------------------------------------------------------------------
    // Tests: invalid configured values
    // -----------------------------------------------------------------------

    @Test
    public void givenClientWithZeroAccessTokenSeconds_whenResolveAccess_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, 0, null);

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenClientWithNegativeAccessTokenSeconds_whenResolveAccess_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, -3600, null);

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenClientWithZeroRefreshTokenSeconds_whenResolveRefresh_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, null, 0);

        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    @Test
    public void givenClientWithNegativeRefreshTokenSeconds_whenResolveRefresh_thenReturnsEmpty() {
        addClientConfig(CLIENT_A, null, -100);

        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(CLIENT_A);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Tests: extension guard (per-client cannot exceed global default)
    // -----------------------------------------------------------------------

    @Test
    public void givenClientAccessExceedsGlobalAndExtensionDisabled_whenResolveAccess_thenCapsAtGlobal() {
        int longerThanGlobal = GLOBAL_ACCESS_SECONDS + 3600;
        addClientConfig(CLIENT_A, longerThanGlobal, null);
        security.setAllowClientTokenLifetimeExtension(false);

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).hasValue(GLOBAL_ACCESS_SECONDS);
    }

    @Test
    public void givenClientRefreshExceedsGlobalAndExtensionDisabled_whenResolveRefresh_thenCapsAtGlobal() {
        int longerThanGlobal = GLOBAL_REFRESH_SECONDS + 3600;
        addClientConfig(CLIENT_A, null, longerThanGlobal);
        security.setAllowClientTokenLifetimeExtension(false);

        Optional<Integer> result = service.resolveClientSpecificRefreshTokenValidity(CLIENT_A);

        assertThat(result).hasValue(GLOBAL_REFRESH_SECONDS);
    }

    @Test
    public void givenClientAccessExceedsGlobalAndExtensionAllowed_whenResolveAccess_thenReturnsClientValue() {
        int longerThanGlobal = GLOBAL_ACCESS_SECONDS + 3600;
        addClientConfig(CLIENT_A, longerThanGlobal, null);
        security.setAllowClientTokenLifetimeExtension(true);

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).hasValue(longerThanGlobal);
    }

    @Test
    public void givenNonPositiveGlobalDefault_whenClientExceedsIt_thenReturnsClientValueUnchanged() {
        // global default of 0 means non-expiring; extension guard must not cap a valid positive value to 0
        security.setAccessTokenValiditySeconds(0);
        security.setAllowClientTokenLifetimeExtension(false);
        int clientAccess = 1800;
        addClientConfig(CLIENT_A, clientAccess, null);

        Optional<Integer> result = service.resolveClientSpecificAccessTokenValidity(CLIENT_A);

        assertThat(result).hasValue(clientAccess);
    }


    // -----------------------------------------------------------------------

    @Test
    public void givenClientSpecificAccessConfig_whenGetAccessTokenValidity_thenReturnsClientValue() {
        int reducedAccess = 1800;
        addClientConfig(CLIENT_A, reducedAccess, null);

        OAuth2Authentication auth = buildAuthentication(CLIENT_A, "user");
        int result = service.getAccessTokenValiditySeconds(auth);

        assertThat(result).isEqualTo(reducedAccess);
    }

    @Test
    public void givenNoClientSpecificConfig_whenGetAccessTokenValidity_thenReturnsGlobalDefault() {
        OAuth2Authentication auth = buildAuthentication(UNKNOWN_CLIENT, "user");
        // ClientDetailsService returns a ClientDetails with null access validity
        ClientDetails clientDetails = mock(ClientDetails.class);
        when(clientDetails.getAccessTokenValiditySeconds()).thenReturn(null);
        when(clientDetailsService.loadClientByClientId(UNKNOWN_CLIENT)).thenReturn(clientDetails);

        int result = service.getAccessTokenValiditySeconds(auth);

        assertThat(result).isEqualTo(GLOBAL_ACCESS_SECONDS);
    }

    @Test
    public void givenClientSpecificAccessConfig_whenGetRefreshTokenValidity_thenReturnsGlobalDefaultForRefresh() {
        // Only access token configured → refresh should fall back to global default (no DB lookup)
        addClientConfig(CLIENT_A, 1800, null);

        OAuth2Authentication auth = buildAuthentication(CLIENT_A, "user");
        int result = service.getRefreshTokenValiditySeconds(auth);

        assertThat(result).isEqualTo(GLOBAL_REFRESH_SECONDS);
    }

    @Test
    public void givenClientSpecificRefreshConfig_whenGetRefreshTokenValidity_thenReturnsClientValue() {
        int reducedRefresh = 43200;
        addClientConfig(CLIENT_A, null, reducedRefresh);

        OAuth2Authentication auth = buildAuthentication(CLIENT_A, "user");
        int result = service.getRefreshTokenValiditySeconds(auth);

        assertThat(result).isEqualTo(reducedRefresh);
    }

    // -----------------------------------------------------------------------
    // Tests: refresh flow uses client-specific access token duration
    // -----------------------------------------------------------------------

    @Test
    public void givenClientSpecificAccessConfig_whenAuthIsRefresh_thenReturnsClientValue() {
        int reducedAccess = 900;
        addClientConfig(CLIENT_B, reducedAccess, null);

        // simulate a refresh_token grant (isRefresh() == true)
        OAuth2Request refreshRequest = new OAuth2Request(
            Collections.singletonMap("grant_type", "refresh_token"),
            CLIENT_B, null, true, null, null, null, null, null
        );
        OAuth2Authentication auth = new OAuth2Authentication(refreshRequest, null);

        int result = service.getAccessTokenValiditySeconds(auth);

        assertThat(result).isEqualTo(reducedAccess);
    }

    // -----------------------------------------------------------------------
    // Tests: multiple clients are independent
    // -----------------------------------------------------------------------

    @Test
    public void givenMultipleClientConfigs_whenResolveSeparately_thenEachClientGetsOwnValue() {
        addClientConfig(CLIENT_A, 1800, 43200);
        addClientConfig(CLIENT_B, 300, 3600);

        assertThat(service.resolveClientSpecificAccessTokenValidity(CLIENT_A)).hasValue(1800);
        assertThat(service.resolveClientSpecificRefreshTokenValidity(CLIENT_A)).hasValue(43200);
        assertThat(service.resolveClientSpecificAccessTokenValidity(CLIENT_B)).hasValue(300);
        assertThat(service.resolveClientSpecificRefreshTokenValidity(CLIENT_B)).hasValue(3600);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void addClientConfig(String clientId, Integer accessSeconds, Integer refreshSeconds) {
        Map<String, TenantProperties.Security.ClientTokenLifetime> lifetimes =
            security.getClientTokenLifetimes();
        if (lifetimes == null) {
            lifetimes = new HashMap<>();
            security.setClientTokenLifetimes(lifetimes);
        }
        TenantProperties.Security.ClientTokenLifetime lifetime =
            new TenantProperties.Security.ClientTokenLifetime();
        lifetime.setAccessTokenValiditySeconds(accessSeconds);
        lifetime.setRefreshTokenValiditySeconds(refreshSeconds);
        lifetimes.put(clientId, lifetime);
    }

    private OAuth2Authentication buildAuthentication(String clientId, String username) {
        OAuth2Request request = new OAuth2Request(
            null, clientId, null, true, null, null, null, null, null
        );
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken
            userAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            username, "n/a", Collections.emptyList()
        );
        return new OAuth2Authentication(request, userAuth);
    }
}
