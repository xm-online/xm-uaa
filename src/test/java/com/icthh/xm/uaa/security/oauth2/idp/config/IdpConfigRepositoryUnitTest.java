package com.icthh.xm.uaa.security.oauth2.idp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.uaa.security.oauth2.idp.IdpTestUtils.buildPublicConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class IdpConfigRepositoryUnitTest {

    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();

    private final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(tenantContextHolder);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void test_shouldNotRegisterAnyTenantClientConfig() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";

        registerPublicConfigs(clientKeyPrefix, tenantKey, 1, false, true);

        assertNull(idpConfigRepository.getIdpClientConfigsByTenantKey(tenantKey));
    }

    @Test
    public void test_shouldSuccessfullyRegisterExactOneTenantClientConfig() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        registerPublicConfigs(clientKeyPrefix, tenantKey, 1, true, true);

        validateInMemoryConfigs(tenantKey, 1);
    }

    @Test
    public void test_shouldSuccessfullyRegisterTwoClientConfigsForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;
        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

        validateInMemoryConfigs(tenantKey, clientsAmount);
    }

    @Test
    public void test_shouldRemoveOneRegisteredClientConfigForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";

        //register two tenant public configs
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;
        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateInMemoryConfigs(tenantKey, clientsAmount);

        //register one tenant config instead of two
        clientsAmount = 1;
        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateInMemoryConfigs(tenantKey, clientsAmount);

        Exception exception = null;
        try {
            idpConfigRepository.getJwtClaimsSetVerifiers("client-id1");
        } catch (Exception t) {
            exception = t;
        }

        assertNotNull(exception);
        assertEquals("Jwt claims verifiers for tenant [tenant1] not found with clientId [client-id1]. " +
            "Check tenant idp configuration.", exception.getMessage());
    }

    @Test
    public void test_shouldFixInMemoryClientConfigurationAndRegisterClientConfigForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";

        //unsuccessful registration for tenant client
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;
        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, true);

        assertNull(idpConfigRepository.getIdpClientConfigsByTenantKey(tenantKey));

        //register one tenant client with full configuration
        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateInMemoryConfigs(tenantKey, clientsAmount);
    }

    @Test
    public void test_shouldRemoveAllRegisteredClientConfigsAndClaimVerifiersForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";

        //register two clients for tenant
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;
        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateInMemoryConfigs(tenantKey, clientsAmount);
        assertEquals(2, idpConfigRepository.getJwtClaimsSetVerifiers("client-id1").size());

        //remove all clients registration for tenant
        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, false);
        assertNull(idpConfigRepository.getIdpClientConfigsByTenantKey(tenantKey));

        Exception exception = null;
        try {
            idpConfigRepository.getJwtClaimsSetVerifiers("client-id0");
        } catch (Exception t) {
            exception = t;
        }

        assertNotNull(exception);
        assertEquals("Jwt claims verifiers for tenant [tenant1] not found with clientId [client-id0]. " +
            "Check tenant idp configuration.", exception.getMessage());
    }

    @Test
    public void test_shouldSuccessfullyRegisterOneClientConfigPerTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateInMemoryConfigs(tenantKey, clientsAmount);

        tenantKey = "tenant2";
        clientKeyPrefix = "Auth0_";

        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateInMemoryConfigs(tenantKey, clientsAmount);
    }

    private void validateInMemoryConfigs(String tenantKey, int clientsAmount) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

        assertNotNull(idpConfigRepository.getIdpClientConfigsByTenantKey(tenantKey));

        for (int i = 0; i < clientsAmount; i++) {
            assertEquals(2, idpConfigRepository.getJwtClaimsSetVerifiers("client-id" + i).size());
        }
    }

    private void registerPublicConfigs(String clientKeyPrefix,
                                       String tenantKey,
                                       int clientsAmount,
                                       boolean buildValidConfig, boolean onInit) throws JsonProcessingException {
        String publicSettingsConfigPath = IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN.replace("{tenant}", tenantKey);

        IdpPublicConfig idpPublicConfig =
            buildPublicConfig(clientKeyPrefix, "client-id", "http://issuer.com",
                clientsAmount, buildValidConfig);

        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);

        if (onInit) {
            idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);
        } else {
            idpConfigRepository.onRefresh(publicSettingsConfigPath, publicConfigAsString);
        }
    }

}
