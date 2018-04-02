package com.icthh.xm.uaa.security;

import static com.icthh.xm.uaa.config.Constants.AUTH_ROLE_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_USER_KEY;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_ALIAS;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_PATH;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_PSWRD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.provider.DefaultAuthenticationRefreshProvider;
import com.icthh.xm.uaa.service.OnlineUsersService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private OnlineUsersService onlineUsersService;

    private DomainTokenServices tokenServices;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));

        TenantContextHolder tenantContextHolder = mock(TenantContextHolder.class);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        JwtAccessTokenConverter converter = new DomainJwtAccessTokenConverter(tenantContextHolder);
        KeyPair keyPair = new KeyStoreKeyFactory(
            new ClassPathResource(KEYSTORE_PATH), KEYSTORE_PSWRD.toCharArray())
            .getKeyPair(KEYSTORE_ALIAS);
        converter.setKeyPair(keyPair);
        converter.afterPropertiesSet();
        TokenStore tokenStore = new JwtTokenStore(converter);

        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
        when(tenantProperties.getSecurity()).thenReturn(security);
        doNothing().when(onlineUsersService).save(any(), any(), anyLong());

        TokenConstraintsService tokenConstraintsService = new TokenConstraintsService();
        tokenConstraintsService.setTenantPropertiesService(tenantPropertiesService);
        tokenConstraintsService.setApplicationProperties(applicationProperties);
        tokenConstraintsService.setSupportRefreshToken(true);

        tokenServices = new DomainTokenServices();
        tokenServices.setTokenStore(tokenStore);
        tokenServices.setTokenEnhancer(converter);
        tokenServices.setTenantPropertiesService(tenantPropertiesService);
        tokenServices.setTenantContextHolder(tenantContextHolder);
        tokenServices.setTokenConstraintsService(tokenConstraintsService);
        tokenServices.setAuthenticationRefreshProvider(new DefaultAuthenticationRefreshProvider());
        tokenServices.setOnlineUsersService(onlineUsersService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAccessToken() throws JsonProcessingException {
        OAuth2AccessToken token = tokenServices.createAccessToken(createAuthentication(LOGIN, TENANT, ROLE));

        assertNotNull(token.getAdditionalInformation());
        assertEquals(TENANT, token.getAdditionalInformation().get(AUTH_TENANT_KEY));
        assertEquals(USER_KEY, token.getAdditionalInformation().get(AUTH_USER_KEY));

        assertEquals(ROLE, token.getAdditionalInformation().get(AUTH_ROLE_KEY));


        OAuth2Authentication auth = tokenServices.loadAuthentication(token.getValue());

        assertEquals(LOGIN, auth.getUserAuthentication().getName());
        assertEquals(CLIENT, auth.getOAuth2Request().getClientId());
        assertEquals(TENANT, ((Map<String, String>) auth.getDetails()).get(AUTH_TENANT_KEY));
        assertEquals(USER_KEY, ((Map<String, String>) auth.getDetails()).get(AUTH_USER_KEY));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRefreshToken() throws Exception {
        OAuth2AccessToken accessToken = tokenServices.createAccessToken(createAuthentication(LOGIN, TENANT, ROLE));

        assertNotNull(accessToken);
        assertNotNull(accessToken.getRefreshToken());
        assertNotNull(accessToken.getRefreshToken().getValue());

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", accessToken.getRefreshToken().getValue());

        TokenRequest tokenRequest = new TokenRequest(params, CLIENT, null, "refresh_token");

        OAuth2AccessToken refreshedToken = tokenServices.refreshAccessToken(accessToken.getRefreshToken().getValue(),
                                                                            tokenRequest);

        assertNotNull(refreshedToken);
        assertNotNull(refreshedToken.getValue());
        assertNotNull(refreshedToken.getRefreshToken());
        assertNotNull(refreshedToken.getRefreshToken().getValue());

        OAuth2Authentication auth = tokenServices.loadAuthentication(refreshedToken.getValue());

        assertEquals(LOGIN, auth.getUserAuthentication().getName());
        assertEquals(CLIENT, auth.getOAuth2Request().getClientId());
        assertEquals(TENANT, ((Map<String, String>) auth.getDetails()).get(AUTH_TENANT_KEY));
        assertEquals(USER_KEY, ((Map<String, String>) auth.getDetails()).get(AUTH_USER_KEY));
    }

    private OAuth2Authentication createAuthentication(String username, String tenant, String role) {
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
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
                                                                                principal.getAuthorities());

        // Create the authorization request and OAuth2Authentication object
        OAuth2Request authRequest = new OAuth2Request(null, CLIENT, null, true, null, null, null, null,
                                                      null);
        return new OAuth2Authentication(authRequest, authentication);
    }
}
