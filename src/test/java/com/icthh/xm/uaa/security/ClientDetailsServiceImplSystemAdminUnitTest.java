package com.icthh.xm.uaa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.service.ClientService;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;

@RunWith(MockitoJUnitRunner.class)
public class ClientDetailsServiceImplSystemAdminUnitTest {

    private static final String SYSTEM_ADMIN_CLIENT_ID = "system-admin";

    @Mock
    private ClientService clientService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private DefaultClientSecretSupplier defaultClientSecret;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private ClientDetailsServiceImpl service;

    @Before
    public void setUp() {
        service = new ClientDetailsServiceImpl(clientService, applicationProperties, passwordEncoder,
            defaultClientSecret);
        when(applicationProperties.getSystemAdminClientId()).thenReturn(SYSTEM_ADMIN_CLIENT_ID);
    }

    @Test
    public void shouldLoadSystemAdminClientWithoutStorage() {
        when(applicationProperties.getSystemAdminClientSecret()).thenReturn("envSecret");
        when(applicationProperties.getClientScope()).thenReturn(Set.of("openid"));

        ClientDetails details = service.loadClientByClientId(SYSTEM_ADMIN_CLIENT_ID);

        assertThat(details.getClientId()).isEqualTo(SYSTEM_ADMIN_CLIENT_ID);
        assertThat(passwordEncoder.matches("envSecret", details.getClientSecret())).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("SYSTEM-ADMIN");
        assertThat(details.getAuthorizedGrantTypes()).containsExactly("client_credentials");
        verifyZeroInteractions(clientService, defaultClientSecret);
    }

    @Test
    public void shouldUseRandomSecretWhenSystemAdminClientSecretIsMissing() {
        when(applicationProperties.getSystemAdminClientSecret()).thenReturn(null);

        ClientDetails details = service.loadClientByClientId(SYSTEM_ADMIN_CLIENT_ID);

        assertThat(details.getClientSecret()).isNotBlank();
        assertThat(passwordEncoder.matches("", details.getClientSecret())).isFalse();
        assertThat(passwordEncoder.matches("null", details.getClientSecret())).isFalse();
    }

    @Test
    public void shouldUseRandomSecretWhenSystemAdminClientSecretIsBlank() {
        when(applicationProperties.getSystemAdminClientSecret()).thenReturn("  ");

        ClientDetails details = service.loadClientByClientId(SYSTEM_ADMIN_CLIENT_ID);

        assertThat(passwordEncoder.matches("  ", details.getClientSecret())).isFalse();
    }
}
