package com.icthh.xm.uaa.security;

import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_USER_KEY;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_ALIAS;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_PATH;
import static com.icthh.xm.uaa.config.Constants.KEYSTORE_PSWRD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import java.security.KeyPair;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.web.client.RestTemplate;

public class DomainTokenServicesUnitTest {

    private static final String TENANT = "testTenant";
    private static final String USER_KEY = "userKey";
    private static final String CLIENT = "testClient";
    private static final String LOGIN = "testLogin";

    @Autowired
    private OAuth2TokenMockUtil util;
    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private TenantProperties.Security security;
    @Mock
    private RestTemplate loadBalancedRestTemplate;

    private TokenStore tokenStore;

    private DomainTokenServices tokenServices;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        JwtAccessTokenConverter converter = new DomainJwtAccessTokenConverter();
        KeyPair keyPair = new KeyStoreKeyFactory(
            new ClassPathResource(KEYSTORE_PATH), KEYSTORE_PSWRD.toCharArray())
            .getKeyPair(KEYSTORE_ALIAS);
        converter.setKeyPair(keyPair);
        converter.afterPropertiesSet();
        tokenStore = new JwtTokenStore(converter);
        tokenServices = new DomainTokenServices();
        tokenServices.setTokenStore(tokenStore);
        tokenServices.setTokenEnhancer(converter);
        tokenServices.setTenantPropertiesService(tenantPropertiesService);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
        when(tenantProperties.getSecurity()).thenReturn(security);
    }

    @Test
    public void test() throws JsonProcessingException {
        OAuth2AccessToken token = tokenServices.createAccessToken(createAuthentication(LOGIN, TENANT));

        assertNotNull(token.getAdditionalInformation());
        assertEquals(TENANT, token.getAdditionalInformation().get(AUTH_TENANT_KEY));
        assertEquals(USER_KEY, token.getAdditionalInformation().get(AUTH_USER_KEY));


        OAuth2Authentication auth = tokenServices.loadAuthentication(token.getValue());

        assertEquals(LOGIN, auth.getUserAuthentication().getName());
        assertEquals(CLIENT, auth.getOAuth2Request().getClientId());
        assertEquals(TENANT, ((Map<String, String>) auth.getDetails()).get(AUTH_TENANT_KEY));
        assertEquals(USER_KEY, ((Map<String, String>) auth.getDetails()).get(AUTH_USER_KEY));
    }

    private OAuth2Authentication createAuthentication(String username, String tenant) {
        DomainUserDetails principal = new DomainUserDetails(username, "test", Collections.emptySet(), tenant, "userKey");
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
            principal.getAuthorities());

        // Create the authorization request and OAuth2Authentication object
        OAuth2Request authRequest = new OAuth2Request(null, CLIENT, null, true, null, null, null, null,
            null);
        return new OAuth2Authentication(authRequest, authentication);
    }
}
