package com.icthh.xm.uaa.security;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.uaa.config.Constants.AUTH_ADDITIONAL_DETAILS;
import static com.icthh.xm.uaa.config.Constants.AUTH_ROLE_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_USER_KEY;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_ALIAS;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_PATH;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_PSWRD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.provider.DefaultAuthenticationRefreshProvider;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

@RunWith(MockitoJUnitRunner.class)
public class DomainTokenServicesUnitTest {

    private static final String TENANT = "testTenant";
    private static final String USER_KEY = "userKey";
    private static final String CLIENT = "testClient";
    private static final String LOGIN = "testLogin";
    private static final String ROLE = "testRole";

    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private TenantProperties.Security security;
    @Mock
    private ApplicationProperties.Security appSecurity;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private ClientDetailsService clientDetailsService;
    @Mock
    private UserService userService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private DefaultAuthenticationRefreshProvider authenticationRefreshProvider;
    @Mock
    private DomainJwtAccessTokenDetailsPostProcessor domainJwtAccessTokenDetailsPostProcessor;

    @InjectMocks
    private UserSecurityValidator userSecurityValidator;

    @InjectMocks
    private TokenConstraintsService tokenConstraintsService;

    @InjectMocks
    private DomainTokenServices tokenServices;

    @Before
    public void setup() throws Exception {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));

        TenantContextHolder tenantContextHolder = mock(TenantContextHolder.class);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        doCallRealMethod().when(domainJwtAccessTokenDetailsPostProcessor).processJwtAccessTokenDetails(any(), any());
        JwtAccessTokenConverter converter = new DomainJwtAccessTokenConverter(tenantContextHolder, domainJwtAccessTokenDetailsPostProcessor);
        KeyPair keyPair = new KeyStoreKeyFactory(
            new ClassPathResource(KEYSTORE_PATH), KEYSTORE_PSWRD.toCharArray())
            .getKeyPair(KEYSTORE_ALIAS);
        converter.setKeyPair(keyPair);
        converter.afterPropertiesSet();
        TokenStore tokenStore = new JwtTokenStore(converter);

        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
        when(tenantProperties.getSecurity()).thenReturn(security);

        when(applicationProperties.getSecurity()).thenReturn(appSecurity);

        tokenServices = new DomainTokenServices();
        tokenServices.setTokenStore(tokenStore);
        tokenServices.setTokenEnhancer(converter);
        tokenServices.setTenantPropertiesService(tenantPropertiesService);
        tokenServices.setTenantContextHolder(tenantContextHolder);
        tokenServices.setTokenConstraintsService(tokenConstraintsService);
        tokenServices.setAuthenticationRefreshProvider(authenticationRefreshProvider);
        tokenServices.setUserService(userService);
        tokenServices.setUserSecurityValidator(userSecurityValidator);
    }

    @Test
    public void testAccessToken() {
        OAuth2AccessToken token = tokenServices.createAccessToken(createAuthentication(LOGIN, TENANT, ROLE));

        assertTokenAttributes(token);
    }

    @Test
    public void testAccessTokenWithAdditionalParameters() {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("param1", "value1");
        OAuth2Authentication authentication = createAuthentication(requestParams, LOGIN, TENANT, ROLE);
        OAuth2AccessToken token = tokenServices.createAccessToken(authentication);

        assertTokenAttributes(token);
        assertEquals(token.getAdditionalInformation().get(AUTH_ADDITIONAL_DETAILS), of("param1", "value1"));
    }

    private void assertTokenAttributes(OAuth2AccessToken token) {
        assertNotNull(token);
        assertNotNull(token.getValue());
        assertNotNull(token.getRefreshToken());
        assertNotNull(token.getRefreshToken().getValue());

        assertNotNull(token.getAdditionalInformation());
        assertEquals(TENANT, token.getAdditionalInformation().get(AUTH_TENANT_KEY));
        assertEquals(USER_KEY, token.getAdditionalInformation().get(AUTH_USER_KEY));

        assertEquals(ROLE, token.getAdditionalInformation().get(AUTH_ROLE_KEY));

        OAuth2Authentication auth = tokenServices.loadAuthentication(token.getValue());

        assertAuthenticationProperties(auth);
    }

    @Test
    public void testRefreshToken() {
        whenFindUserByLogin(true);

        when(authenticationRefreshProvider.refresh(any(OAuth2Authentication.class))).thenCallRealMethod();

        OAuth2AccessToken accessToken = tokenServices.createAccessToken(createAuthentication(LOGIN, TENANT, ROLE));
        assertTokenAttributes(accessToken);

        String refreshTokenValue = accessToken.getRefreshToken().getValue();

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshTokenValue);

        TokenRequest tokenRequest = new TokenRequest(params, CLIENT, null, "refresh_token");

        OAuth2AccessToken refreshedToken = tokenServices.refreshAccessToken(refreshTokenValue, tokenRequest);
        assertTokenAttributes(refreshedToken);
    }

    @Test
    public void testRefreshTokenWithAdditionalDetails() {
        whenFindUserByLogin(true);

        when(authenticationRefreshProvider.refresh(any(OAuth2Authentication.class))).thenCallRealMethod();

        OAuth2AccessToken accessToken = tokenServices
            .createAccessToken(createAuthentication(LOGIN, TENANT, ROLE, of("detail_key", "detail_value")));
        assertTokenAttributes(accessToken);

        assertEquals("detail_value", getAdditionalDetailByKey(accessToken.getAdditionalInformation(), "detail_key"));

        // verify that additional details passed to JWT details postprocessor during access token creation
        DomainUserDetails domainUserDetails = verifyJwtAccessTokenDetailsPostProcessor(1, false);
        assertEquals("detail_value", domainUserDetails.getAdditionalDetails().get("detail_key"));

        String refreshTokenValue = accessToken.getRefreshToken().getValue();

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshTokenValue);

        TokenRequest tokenRequest = new TokenRequest(params, CLIENT, null, "refresh_token");

        OAuth2AccessToken refreshedToken = tokenServices.refreshAccessToken(refreshTokenValue, tokenRequest);
        assertTokenAttributes(refreshedToken);

        // verify that additional details passed to JWT details postprocessor during access token refresh
        domainUserDetails = verifyJwtAccessTokenDetailsPostProcessor(2, true);
        assertEquals("detail_value", domainUserDetails.getAdditionalDetails().get("detail_key"));

        OAuth2Authentication auth = tokenServices.loadAuthentication(refreshedToken.getValue());
        assertEquals("detail_value", getAdditionalDetailByKey(auth.getDetails(), "detail_key"));

    }

    @Test
    public void testRefreshTokenForDeactivatedUser() {
        whenFindUserByLogin(false);

        OAuth2AccessToken accessToken = tokenServices.createAccessToken(createAuthentication(LOGIN, TENANT, ROLE));

        assertNotNull(accessToken);
        assertNotNull(accessToken.getRefreshToken());
        assertNotNull(accessToken.getRefreshToken().getValue());

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", accessToken.getRefreshToken().getValue());

        TokenRequest tokenRequest = new TokenRequest(params, CLIENT, null, "refresh_token");
        try {
            tokenServices.refreshAccessToken(accessToken.getRefreshToken().getValue(), tokenRequest);
            fail("Expected UserNotActivatedException to be thrown");
        } catch (UserNotActivatedException e) {
            // ignore
        }

        verifyNoMoreInteractions(authenticationManager);
        verifyNoMoreInteractions(authenticationRefreshProvider);
    }

    private DomainUserDetails verifyJwtAccessTokenDetailsPostProcessor(int times, boolean isRefresh) {
        ArgumentCaptor<OAuth2Authentication> captor = ArgumentCaptor.forClass(OAuth2Authentication.class);

        verify(domainJwtAccessTokenDetailsPostProcessor, times(times))
            .processJwtAccessTokenDetails(captor.capture(), any());

        assertEquals(isRefresh, captor.getValue().getOAuth2Request().isRefresh());

        return (DomainUserDetails) captor.getValue().getUserAuthentication().getPrincipal();

    }

    private void assertAuthenticationProperties(OAuth2Authentication auth) {
        assertEquals(LOGIN, auth.getPrincipal());
        assertEquals(LOGIN, auth.getUserAuthentication().getName());
        assertEquals(CLIENT, auth.getOAuth2Request().getClientId());
        assertEquals(TENANT, getDetailByKey(auth.getDetails(), AUTH_TENANT_KEY));
        assertEquals(USER_KEY, getDetailByKey(auth.getDetails(),AUTH_USER_KEY));
    }

    private void whenFindUserByLogin(boolean isActive) {
        when(userService.findOneByLogin(LOGIN)).thenAnswer(invocation -> {
            User user = new User();
            user.setActivated(isActive); // user is active
            user.setUserKey(USER_KEY);
            return Optional.of(user);
        });
    }

    private Object getDetailByKey(Object details, String key) {
        return Optional.ofNullable(details)
                       .map(Map.class::cast)
                       .map(map -> map.get(key))
                       .orElseThrow(() -> new RuntimeException(" details does not contains value for key: " + key));
    }

    private Object getAdditionalDetailByKey(Object details, String key) {
        return Optional.ofNullable(details)
                       .map(Map.class::cast)
                       .map(map -> map.get(AUTH_ADDITIONAL_DETAILS))
                       .map(Map.class::cast)
                       .map(map -> map.get(key))
                       .orElseThrow(() -> new RuntimeException(
                           AUTH_ADDITIONAL_DETAILS + " does not contains value for key: " + key));
    }

    private OAuth2Authentication createAuthentication(String username, String tenant, String role, Map<String, String> additionalDetails) {
        return createAuthentication(null, username, tenant, role, additionalDetails);
    }

    private OAuth2Authentication createAuthentication(Map<String, String> requestParams, String username, String tenant, String role) {
        return createAuthentication(requestParams, username, tenant, role, Collections.emptyMap());
    }

    private OAuth2Authentication createAuthentication(String username, String tenant, String role) {
        return createAuthentication(null, username, tenant, role, Collections.emptyMap());
    }
    private OAuth2Authentication createAuthentication(Map<String, String> requestParams, String username, String tenant, String role, Map<String, String> additionalDetails) {
        DomainUserDetails principal = new DomainUserDetails(username,
                                                            "test",
                                                            Collections.singletonList(new SimpleGrantedAuthority(role)),
                                                            tenant,
                                                            "userKey",
                                                            false,
                                                            null,
                                                            null,
                                                            false,
                                                            null);
        principal.getAdditionalDetails().putAll(additionalDetails);

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
                                                                                principal.getAuthorities());

        // Create the authorization request and OAuth2Authentication object
        OAuth2Request authRequest = new OAuth2Request(requestParams, CLIENT, null, true, null, null, null, null,
                                                      null);
        return new OAuth2Authentication(authRequest, authentication);
    }
}
