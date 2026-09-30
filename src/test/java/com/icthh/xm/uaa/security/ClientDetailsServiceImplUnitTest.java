package com.icthh.xm.uaa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;

import java.util.Optional;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class ClientDetailsServiceImplUnitTest {

    private static final String DEFAULT_CLIENT_ID = "internal";

    @Mock
    private ClientService clientService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private TenantContext tenantContext;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final TenantProperties.Security security = new TenantProperties.Security();

    private ClientDetailsServiceImpl service;

    @Before
    public void setUp() {
        service = new ClientDetailsServiceImpl(clientService, applicationProperties, passwordEncoder,
            tenantPropertiesService, tenantContextHolder);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setSecurity(security);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
        when(applicationProperties.getDefaultClientId()).thenReturn(Set.of(DEFAULT_CLIENT_ID));
    }

    @Test
    public void shouldUseTenantDefaultClientSecret() {
        security.setDefaultClientSecret("tenantSecret");

        ClientDetails details = service.loadClientByClientId(DEFAULT_CLIENT_ID);

        assertThat(passwordEncoder.matches("tenantSecret", details.getClientSecret())).isTrue();
    }

    @Test
    public void shouldUseRandomSecretWhenTenantDefaultClientSecretIsMissing() {
        security.setDefaultClientSecret(null);

        ClientDetails details = service.loadClientByClientId(DEFAULT_CLIENT_ID);

        assertThat(details.getClientSecret()).isNotBlank();
        assertThat(passwordEncoder.matches(DEFAULT_CLIENT_ID, details.getClientSecret())).isFalse();
        assertThat(passwordEncoder.matches("", details.getClientSecret())).isFalse();
    }

    @Test
    public void shouldUseRandomSecretWhenTenantDefaultClientSecretIsBlank() {
        security.setDefaultClientSecret("  ");

        ClientDetails details = service.loadClientByClientId(DEFAULT_CLIENT_ID);

        assertThat(passwordEncoder.matches("  ", details.getClientSecret())).isFalse();
    }

    @Test
    public void shouldStillUseWellKnownTenantDefaultClientSecret() {
        security.setDefaultClientSecret("internal");

        ClientDetails details = service.loadClientByClientId(DEFAULT_CLIENT_ID);

        assertThat(passwordEncoder.matches("internal", details.getClientSecret())).isTrue();
    }
}
