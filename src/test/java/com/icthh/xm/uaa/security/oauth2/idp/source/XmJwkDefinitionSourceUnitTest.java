package com.icthh.xm.uaa.security.oauth2.idp.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.repository.JwksRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.JwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.JwkDefinitionSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.domain.idp.IdpConstants.PUBLIC_JWKS_CONFIG_PATTERN;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.buildJWKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class XmJwkDefinitionSourceUnitTest {

    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();

    private final JwksRepository jwksRepository = new JwksRepository(tenantContextHolder);

    private final XmJwkDefinitionSource xmJwkDefinitionSource = new XmJwkDefinitionSource(jwksRepository);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void setUp() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void test_shouldFailToFindJwkDefinitionHolder() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        String keyId = "ozRd0JdfWHAmxvtcbqpxX";

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        registerJwksConfigs(clientKeyPrefix, tenantKey, true, true);

        JwkDefinitionSource.JwkDefinitionHolder jwkDefinitionHolder =
            xmJwkDefinitionSource.getDefinitionLoadIfNecessary(keyId);

        assertNull(jwkDefinitionHolder);
    }

    @Test
    public void test_shouldSuccessfullyBuildJwkDefinitionHolder() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        String keyId = "ozRd0JdbWHAmxvtcbqpxX";

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        registerJwksConfigs(clientKeyPrefix, tenantKey, true, true);

        JwkDefinitionSource.JwkDefinitionHolder jwkDefinitionHolder =
            xmJwkDefinitionSource.getDefinitionLoadIfNecessary(keyId);
        assertNotNull(jwkDefinitionHolder);

        SignatureVerifier signatureVerifier = jwkDefinitionHolder.getSignatureVerifier();
        assertNotNull(signatureVerifier);
        assertEquals("SHA256withRSA", signatureVerifier.algorithm());
        JwkDefinition jwkDefinition = jwkDefinitionHolder.getJwkDefinition();

        assertNotNull(jwkDefinition);
        assertEquals(keyId, jwkDefinition.getKeyId());
        assertEquals(JwkDefinition.KeyType.RSA, jwkDefinition.getKeyType());
        assertEquals(JwkDefinition.PublicKeyUse.SIG, jwkDefinition.getPublicKeyUse());
        assertEquals(JwkDefinition.CryptoAlgorithm.RS256, jwkDefinition.getAlgorithm());
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
