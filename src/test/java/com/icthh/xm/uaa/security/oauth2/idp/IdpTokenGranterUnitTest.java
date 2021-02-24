package com.icthh.xm.uaa.security.oauth2.idp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.repository.JwksRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.security.ClientDetailsImpl;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.converter.XmJwkVerifyingJwtAccessTokenConverter;
import com.icthh.xm.uaa.security.oauth2.idp.source.XmJwkDefinitionSource;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.PUBLIC_JWKS_CONFIG_PATTERN;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.buildJWKS;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.getIdToken;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.buildIdpPublicClientConfig;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTokenGranter.GRANT_TYPE_IDP_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdpTokenGranterUnitTest {
    @Mock
    private AuthorizationServerTokenServices tokenServices;
    @Mock
    private ClientDetailsService clientDetailsService;
    @Mock
    private OAuth2RequestFactory requestFactory;
    @Mock
    private DomainUserDetailsService domainUserDetailsService;
    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private UserService userService;
    @Mock
    private UserLoginRepository userLoginRepository;

    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();
    private final JwksRepository jwksRepository = new JwksRepository(tenantContextHolder);
    private final XmJwkDefinitionSource xmJwkDefinitionSource = new XmJwkDefinitionSource(jwksRepository);
    private final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(tenantContextHolder);

    private final XmJwkVerifyingJwtAccessTokenConverter xmJwkVerifyingJwtAccessTokenConverter =
        new XmJwkVerifyingJwtAccessTokenConverter(xmJwkDefinitionSource, idpConfigRepository);

    private final XmJwkTokenStore jwkTokenStore = new XmJwkTokenStore(xmJwkVerifyingJwtAccessTokenConverter);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void setUp() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void test_Success() throws JsonProcessingException {
        String tenantKey = instantiateConfigs();

        UserLoginService userLoginService = new UserLoginService(userLoginRepository);

        when(userLoginRepository.findOneByLoginIgnoreCase(eq("devtest046@gmail.com"))).thenReturn(Optional.empty());

        TenantProperties tenantProps = buildTenantProps();
        tenantProps.getSecurity().setDefaultUserRole("SUPER-ADMIN");
        tenantProps.getSecurity().setIdp(buildIdpUserMappingConfig());
        when(tenantPropertiesService.getTenantContextHolder()).thenReturn(tenantContextHolder);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProps);
        when(userService.createUser(any())).thenReturn(buildUser());

        IdpTokenGranter idpTokenGranter =
            new IdpTokenGranter(
                tokenServices,
                clientDetailsService,
                requestFactory,
                jwkTokenStore,
                domainUserDetailsService,
                tenantPropertiesService,
                userService,
                userLoginService);

        ClientDetails client = buildClientDetails();
        TokenRequest tokenRequest = buildTokenRequest();

        OAuth2Authentication oAuth2Authentication = idpTokenGranter.getOAuth2Authentication(client, tokenRequest);
        Authentication userAuthentication = oAuth2Authentication.getUserAuthentication();
        DomainUserDetails principal = (DomainUserDetails) userAuthentication.getPrincipal();

        assertEquals(tenantKey, principal.getTenant());
        assertEquals("test", principal.getUserKey());
        assertEquals("devtest046@gmail.com", principal.getUsername());
        assertEquals("password", principal.getPassword());
        assertEquals(1, principal.getAuthorities().size());
        assertEquals("SUPER-ADMIN", principal.getAuthorities().iterator().next().getAuthority());

        Collection<? extends GrantedAuthority> authorities = userAuthentication.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("SUPER-ADMIN", authorities.iterator().next().getAuthority());

        Map<String, String> details = (Map<String, String>) userAuthentication.getDetails();
        assertNotNull(details);
        assertEquals(2, details.size());
        assertEquals(getIdToken(), details.get("token"));
        assertEquals(GRANT_TYPE_IDP_TOKEN, details.get("grant_type"));
    }

    @Test
    public void test_FailToMapIdpIdTokenToRole() throws JsonProcessingException {
        instantiateConfigs();

        UserLoginService userLoginService = new UserLoginService(userLoginRepository);

        when(userLoginRepository.findOneByLoginIgnoreCase(eq("devtest046@gmail.com"))).thenReturn(Optional.empty());

        TenantProperties tenantProps = buildTenantProps();
        tenantProps.getSecurity().setIdp(buildIdpUserMappingConfig());
        when(tenantPropertiesService.getTenantContextHolder()).thenReturn(tenantContextHolder);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProps);

        IdpTokenGranter idpTokenGranter =
            new IdpTokenGranter(
                tokenServices,
                clientDetailsService,
                requestFactory,
                jwkTokenStore,
                domainUserDetailsService,
                tenantPropertiesService,
                userService,
                userLoginService);

        ClientDetails client = buildClientDetails();
        TokenRequest tokenRequest = buildTokenRequest();

        Exception exception = null;
        try {
            idpTokenGranter.getOAuth2Authentication(client, tokenRequest);
        } catch (Exception t) {
            exception = t;
        }

        assertNotNull(exception);
        assertEquals("Authentication failed cause of tenant [tenant4] configuration lack.", exception.getMessage());
    }

    @Test
    public void test_FailToGetDefaultClaimsMapping() throws JsonProcessingException {
        instantiateConfigs();

        UserLoginService userLoginService = new UserLoginService(userLoginRepository);

        TenantProperties tenantProps = buildTenantProps();
        when(tenantPropertiesService.getTenantContextHolder()).thenReturn(tenantContextHolder);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProps);

        IdpTokenGranter idpTokenGranter =
            new IdpTokenGranter(
                tokenServices,
                clientDetailsService,
                requestFactory,
                jwkTokenStore,
                domainUserDetailsService,
                tenantPropertiesService,
                userService,
                userLoginService);

        ClientDetails client = buildClientDetails();
        TokenRequest tokenRequest = buildTokenRequest();

        Exception exception = null;
        try {
            idpTokenGranter.getOAuth2Authentication(client, tokenRequest);
        } catch (Exception t) {
            exception = t;
        }

        assertNotNull(exception);
        assertEquals("Authentication failed cause of tenant [tenant4] configuration lack.", exception.getMessage());
    }

    private String instantiateConfigs() throws JsonProcessingException {
        String tenantKey = "tenant4";
        String clientKeyPrefix = "Auth0_";
        String clientId = "I4h5vnAEDwXAnkpun2b9mq3bywHsp71w";
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

        IdpPublicConfig idpPublicConfig = buildPublicConfig(clientKeyPrefix, clientId, true);
        registerPublicConfigs(tenantKey, idpPublicConfig, true);
        registerJwksConfigs(clientKeyPrefix, tenantKey, true, true);
        return tenantKey;
    }

    private User buildUser() {
        User user = new User();
        user.setActivated(true);
        user.setUserKey("test");
        user.setPassword("password");
        user.setRoleKey(RoleConstant.SUPER_ADMIN);

        UserLogin userLogin = new UserLogin();
        userLogin.setLogin("admin");

        return user;
    }

    private TenantProperties buildTenantProps() {

        TenantProperties.Security security = new TenantProperties.Security();
        TenantProperties tenantProps = new TenantProperties();
        tenantProps.setSecurity(security);

        return tenantProps;
    }

    private TenantProperties.Security.Idp buildIdpUserMappingConfig() {
        TenantProperties.Security.Idp idp = new TenantProperties.Security.Idp();
        TenantProperties.Security.Idp.DefaultIdpClaimMapping defaultIdpClaimMapping =
            new TenantProperties.Security.Idp.DefaultIdpClaimMapping();
        defaultIdpClaimMapping.setFirstNameAttribute("given_name");
        defaultIdpClaimMapping.setLastNameAttribute("family_name");
        defaultIdpClaimMapping.setUserIdentityAttribute("email");
        defaultIdpClaimMapping.setUserIdentityType("LOGIN.EMAIL");

        idp.setDefaultIdpClaimMapping(defaultIdpClaimMapping);

        return idp;
    }

    private ClientDetails buildClientDetails() {
        Client client = new Client();
        client.setRoleKey("admin");
        Set<String> scope = Set.of("openid");
        Set<String> grantTypes = Set.of("authorization_code");

        return new ClientDetailsImpl(client, grantTypes, scope);
    }

    private TokenRequest buildTokenRequest() {
        Map<String, String> requestParameters = new HashMap<>();

        requestParameters.put("token", getIdToken());
        requestParameters.put("grant_type", GRANT_TYPE_IDP_TOKEN);

        String clientId = "client-id";
        Collection<String> scope = List.of("openid");
        String grantType = "authorization_code";

        return new TokenRequest(requestParameters, clientId, scope, grantType);
    }

    private IdpPublicConfig buildPublicConfig(String clientKeyPrefix, String clientId, boolean buildValidConfig) throws JsonProcessingException {
        IdpPublicConfig idpPublicConfig =
            IdpTestUtils.buildPublicConfig(clientKeyPrefix, clientId, "", 0, buildValidConfig);

        idpPublicConfig
            .getConfig()
            .setClients(
                List.of(buildIdpPublicClientConfig(clientKeyPrefix + "0", clientId, "https://unit-test.eu.auth0.com/"))
            );

        return idpPublicConfig;
    }

    private void registerJwksConfigs(String clientKeyPrefix,
                                     String tenantKey,
                                     boolean buildValidConfig,
                                     boolean onInit) throws JsonProcessingException {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        String publicSettingsConfigPath = PUBLIC_JWKS_CONFIG_PATTERN
            .replace("{tenant}", tenantKey)
            .replace("{idpClientKey}", clientKeyPrefix + "0");

        Map<String, List<Map<String, Object>>> jwks = buildJWKS(buildValidConfig);

        String jwksAsString = objectMapper.writeValueAsString(jwks);

        if (onInit) {
            jwksRepository.onInit(publicSettingsConfigPath, jwksAsString);
        } else {
            jwksRepository.onRefresh(publicSettingsConfigPath, jwksAsString);
        }
    }

    private void registerPublicConfigs(String tenantKey,
                                       IdpPublicConfig idpPublicConfig,
                                       boolean onInit) throws JsonProcessingException {
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        String publicSettingsConfigPath = IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN.replace("{tenant}", tenantKey);

        if (onInit) {
            idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);
        } else {
            idpConfigRepository.onRefresh(publicSettingsConfigPath, publicConfigAsString);
        }
    }
}
