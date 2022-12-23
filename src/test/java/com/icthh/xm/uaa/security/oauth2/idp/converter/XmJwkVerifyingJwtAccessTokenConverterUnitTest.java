package com.icthh.xm.uaa.security.oauth2.idp.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.repository.JwksRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.source.XmJwkDefinitionSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.PUBLIC_JWKS_CONFIG_PATTERN;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.buildJWKS;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.getIdToken;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.buildIdpPublicClientConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class XmJwkVerifyingJwtAccessTokenConverterUnitTest {

    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();
    private final JwksRepository jwksRepository = new JwksRepository(tenantContextHolder);
    private final XmJwkDefinitionSource xmJwkDefinitionSource = new XmJwkDefinitionSource(jwksRepository);
    private final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(tenantContextHolder);

    private final XmJwkVerifyingJwtAccessTokenConverter xmJwkVerifyingJwtAccessTokenConverter =
        new XmJwkVerifyingJwtAccessTokenConverter(xmJwkDefinitionSource, idpConfigRepository);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void setUp() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void test_shouldFailToFindTokenDefaultClaimsVerifiers() throws JsonProcessingException {
        String tenantKey = "tenant3";
        String clientKeyPrefix = "Auth0_";
        String clientId = "I4h5vnAEDwXAnkpun2b9mq3bywHsp711";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(clientKeyPrefix, clientId, true);

        registerPublicConfigs(tenantKey, idpPublicConfig, true);
        registerJwksConfigs(clientKeyPrefix, tenantKey, true, true);

        String token = getIdToken();

        Exception exception = null;
        try {
            xmJwkVerifyingJwtAccessTokenConverter.decode(token);
        } catch (Exception t) {
            exception = t;
        }

        assertNotNull(exception);
        assertEquals("Jwt claims verifiers for tenant [tenant3] not found with clientId " +
            "[I4h5vnAEDwXAnkpun2b9mq3bywHsp71w]. Check tenant idp configuration.", exception.getMessage());
    }

    @Test
    public void test_shouldSuccessfullyDecodeAndValidateToken() throws JsonProcessingException {
        String tenantKey = "tenant3";
        String clientKeyPrefix = "Auth0_";
        String clientId = "I4h5vnAEDwXAnkpun2b9mq3bywHsp71w";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(clientKeyPrefix, clientId, true);

        registerPublicConfigs(tenantKey, idpPublicConfig, true);
        registerJwksConfigs(clientKeyPrefix, tenantKey, true, true);

        String token = getIdToken();
        Map<String, Object> claims = xmJwkVerifyingJwtAccessTokenConverter.decode(token);

        assertNotNull(claims);
        assertEquals("dev", claims.get("given_name"));
        assertEquals("test", claims.get("family_name"));
        assertEquals("devtest046", claims.get("nickname"));
        assertEquals("dev test", claims.get("name"));
        assertEquals("en", claims.get("locale"));
        assertEquals("devtest046@gmail.com", claims.get("email"));
        assertTrue((Boolean) claims.get("email_verified"));
        assertEquals("https://unit-test.eu.auth0.com/", claims.get("iss"));
        assertEquals("I4h5vnAEDwXAnkpun2b9mq3bywHsp71w", claims.get("aud"));
        assertEquals("1614072948", String.valueOf(claims.get("iat")));
        assertEquals("1614108948", String.valueOf(claims.get("exp")));
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
                                     boolean buildValidConfig, boolean onInit) throws JsonProcessingException {
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

}
