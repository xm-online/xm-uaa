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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.util.Collections;

/**
 * Unit tests for per-client token lifetime resolution in {@link TokenConstraintsService}.
 *
 * Per-client overrides are stored in the {@code Client} DB entity and surfaced through
 * {@link ClientDetails#getAccessTokenValiditySeconds()} /
 * {@link ClientDetails#getRefreshTokenValiditySeconds()}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TokenConstraintsServiceUnitTest {

    /** 24 h — mirrors the typical tenant-level default set in external uaa.yaml */
    private static final int GLOBAL_ACCESS_SECONDS = 86_400;
    /** 90 d — mirrors the typical tenant-level default set in external uaa.yaml */
    private static final int GLOBAL_REFRESH_SECONDS = 7_776_000;

    private static final String CLIENT_A = "client-a";
    private static final String UNKNOWN_CLIENT = "unknown-client";

    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ClientDetailsService clientDetailsService;

    private TokenConstraintsService service;

    @Before
    public void setUp() {
        service = new TokenConstraintsService(tenantPropertiesService, applicationProperties, clientDetailsService);

        TenantProperties.Security security = new TenantProperties.Security();
        security.setAccessTokenValiditySeconds(GLOBAL_ACCESS_SECONDS);
        security.setRefreshTokenValiditySeconds(GLOBAL_REFRESH_SECONDS);

        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setSecurity(security);

        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        ApplicationProperties.Security appSecurity = new ApplicationProperties.Security();
        when(applicationProperties.getSecurity()).thenReturn(appSecurity);
    }

    // -----------------------------------------------------------------------
    // Tests: no client-specific config → global defaults are used
    // -----------------------------------------------------------------------

    @Test
    public void givenNoClientAccessConfig_whenGetAccessTokenValidity_thenReturnsGlobalDefault() {
        ClientDetails clientDetails = clientWithValidity(null, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getAccessTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(GLOBAL_ACCESS_SECONDS);
    }

    @Test
    public void givenNoClientRefreshConfig_whenGetRefreshTokenValidity_thenReturnsGlobalDefault() {
        ClientDetails clientDetails = clientWithValidity(null, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getRefreshTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(GLOBAL_REFRESH_SECONDS);
    }

    // -----------------------------------------------------------------------
    // Tests: client-specific access token config
    // -----------------------------------------------------------------------

    @Test
    public void givenClientAccessConfig_whenGetAccessTokenValidity_thenReturnsClientValue() {
        int clientAccess = 1800; // 30 minutes
        ClientDetails clientDetails = clientWithValidity(clientAccess, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getAccessTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(clientAccess);
    }

    @Test
    public void givenClientAccessConfigOnly_whenGetRefreshTokenValidity_thenReturnsGlobalDefault() {
        // access configured on client, refresh is null → refresh falls back to global
        ClientDetails clientDetails = clientWithValidity(1800, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getRefreshTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(GLOBAL_REFRESH_SECONDS);
    }

    // -----------------------------------------------------------------------
    // Tests: client-specific refresh token config
    // -----------------------------------------------------------------------

    @Test
    public void givenClientRefreshConfig_whenGetRefreshTokenValidity_thenReturnsClientValue() {
        int clientRefresh = 43200; // 12 hours
        ClientDetails clientDetails = clientWithValidity(null, clientRefresh);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getRefreshTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(clientRefresh);
    }

    @Test
    public void givenClientRefreshConfigOnly_whenGetAccessTokenValidity_thenReturnsGlobalDefault() {
        // refresh configured on client, access is null → access falls back to global
        ClientDetails clientDetails = clientWithValidity(null, 43200);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getAccessTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(GLOBAL_ACCESS_SECONDS);
    }

    // -----------------------------------------------------------------------
    // Tests: both access and refresh configured on client
    // -----------------------------------------------------------------------

    @Test
    public void givenBothClientConfigs_whenGetValidity_thenBothReturnClientValues() {
        int clientAccess = 900;
        int clientRefresh = 28800;
        ClientDetails clientDetails = clientWithValidity(clientAccess, clientRefresh);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        assertThat(service.getAccessTokenValiditySeconds(buildAuthentication(CLIENT_A))).isEqualTo(clientAccess);
        assertThat(service.getRefreshTokenValiditySeconds(buildAuthentication(CLIENT_A))).isEqualTo(clientRefresh);
    }

    // -----------------------------------------------------------------------
    // Tests: client exists but has null validity → global defaults are used
    // -----------------------------------------------------------------------

    @Test
    public void givenClientWithNullAccessValidity_whenGetAccessTokenValidity_thenReturnsGlobalDefault() {
        ClientDetails clientDetails = clientWithValidity(null, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getAccessTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(GLOBAL_ACCESS_SECONDS);
    }

    @Test
    public void givenClientWithNullRefreshValidity_whenGetRefreshTokenValidity_thenReturnsGlobalDefault() {
        ClientDetails clientDetails = clientWithValidity(null, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getRefreshTokenValiditySeconds(buildAuthentication(CLIENT_A));

        assertThat(result).isEqualTo(GLOBAL_REFRESH_SECONDS);
    }

    @Test
    public void givenClientDetailsServiceReturnsNull_whenGetAccessTokenValidity_thenReturnsGlobalDefault() {
        // loadClientByClientId returns null → loadClientInfo resolves to Optional.empty()
        when(clientDetailsService.loadClientByClientId(UNKNOWN_CLIENT)).thenReturn(null);

        int result = service.getAccessTokenValiditySeconds(buildAuthentication(UNKNOWN_CLIENT));

        assertThat(result).isEqualTo(GLOBAL_ACCESS_SECONDS);
    }

    @Test
    public void givenClientDetailsServiceReturnsNull_whenGetRefreshTokenValidity_thenReturnsGlobalDefault() {
        when(clientDetailsService.loadClientByClientId(UNKNOWN_CLIENT)).thenReturn(null);

        int result = service.getRefreshTokenValiditySeconds(buildAuthentication(UNKNOWN_CLIENT));

        assertThat(result).isEqualTo(GLOBAL_REFRESH_SECONDS);
    }

    // -----------------------------------------------------------------------
    // Tests: refresh flow also uses client-specific values
    // -----------------------------------------------------------------------

    @Test
    public void givenClientRefreshConfig_whenRefreshGrant_thenReturnsClientValue() {
        int clientRefresh = 3600;
        ClientDetails clientDetails = clientWithValidity(null, clientRefresh);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        OAuth2Request refreshRequest = new OAuth2Request(
            Collections.singletonMap("grant_type", "refresh_token"),
            CLIENT_A, null, true, null, null, null, null, null
        );
        OAuth2Authentication auth = new OAuth2Authentication(refreshRequest, null);

        int result = service.getRefreshTokenValiditySeconds(auth);

        assertThat(result).isEqualTo(clientRefresh);
    }

    // -----------------------------------------------------------------------
    // Tests: user principal present but no user-level validity → client DB wins
    // -----------------------------------------------------------------------

    @Test
    public void givenUserPrincipalWithNoUserValidity_whenClientHasAccessConfig_thenReturnsClientValue() {
        int clientAccess = 1800;
        ClientDetails clientDetails = clientWithValidity(clientAccess, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        // DomainUserDetails with null accessTokenValiditySeconds
        int result = service.getAccessTokenValiditySeconds(buildAuthenticationWithUserPrincipal(CLIENT_A, null));

        assertThat(result).isEqualTo(clientAccess);
    }

    @Test
    public void givenUserPrincipalWithUserValidity_whenClientHasAccessConfig_thenUserValueWins() {
        int userAccess = 300;
        int clientAccess = 1800;
        ClientDetails clientDetails = clientWithValidity(clientAccess, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getAccessTokenValiditySeconds(buildAuthenticationWithUserPrincipal(CLIENT_A, userAccess));

        assertThat(result).isEqualTo(userAccess);
    }

    @Test
    public void givenUserPrincipalWithNoUserValidity_whenClientHasNoAccessConfig_thenReturnsGlobalDefault() {
        ClientDetails clientDetails = clientWithValidity(null, null);
        when(clientDetailsService.loadClientByClientId(CLIENT_A)).thenReturn(clientDetails);

        int result = service.getAccessTokenValiditySeconds(buildAuthenticationWithUserPrincipal(CLIENT_A, null));

        assertThat(result).isEqualTo(GLOBAL_ACCESS_SECONDS);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ClientDetails clientWithValidity(Integer accessSeconds, Integer refreshSeconds) {
        ClientDetails cd = mock(ClientDetails.class);
        when(cd.getAccessTokenValiditySeconds()).thenReturn(accessSeconds);
        when(cd.getRefreshTokenValiditySeconds()).thenReturn(refreshSeconds);
        return cd;
    }

    private OAuth2Authentication buildAuthentication(String clientId) {
        OAuth2Request request = new OAuth2Request(
            null, clientId, null, true, null, null, null, null, null
        );
        UsernamePasswordAuthenticationToken userAuth =
            new UsernamePasswordAuthenticationToken("user", "n/a", Collections.emptyList());
        return new OAuth2Authentication(request, userAuth);
    }

    private OAuth2Authentication buildAuthenticationWithUserPrincipal(String clientId,
                                                                       Integer userAccessTokenValiditySeconds) {
        OAuth2Request request = new OAuth2Request(
            null, clientId, null, true, null, null, null, null, null
        );
        DomainUserDetails principal = new DomainUserDetails(
            "user", "n/a", Collections.emptyList(),
            "TEST", "userKey", false,
            null, null,
            userAccessTokenValiditySeconds, null, null,
            false, null, Collections.emptyList()
        );
        UsernamePasswordAuthenticationToken userAuth =
            new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities());
        return new OAuth2Authentication(request, userAuth);
    }
}
