package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.TreeMap;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.VALIDATION_ROLE_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceUnitTest {

    @InjectMocks
    private ClientService clientService;
    @Mock
    private RoleService roleService;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ApplicationProperties applicationProperties;

    @Test
    void shouldCreateClientWhenRoleExistsInConfiguration() {
        ClientDTO dto = new ClientDTO();
        dto.setClientId("client");
        dto.setClientSecret("secret");
        dto.setRoleKey("USER");

        when(roleService.getRoles(DEFAULT_TENANT_KEY_VALUE)).thenReturn(new TreeMap<>(Map.of("USER", new Role())));
        when(tenantContextHolder.getTenantKey()).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clientService.createClient(dto);

        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void shouldThrowExceptionWhenRoleDoesNotExist() {
        ClientDTO dto = new ClientDTO();
        dto.setClientId("client");
        dto.setRoleKey("UNKNOWN");

        when(tenantContextHolder.getTenantKey()).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(roleService.getRoles(DEFAULT_TENANT_KEY_VALUE)).thenReturn(new TreeMap<>(Map.of("USER", new Role())));

        BusinessException exception = assertThrows(BusinessException.class, () -> clientService.createClient(dto));

        assertEquals(VALIDATION_ROLE_NOT_ALLOWED, exception.getCode());

        verify(clientRepository, never()).save(any());
    }

    @Test
    void shouldAllowSuperAdminWhenFeatureEnabled() {
        ClientDTO dto = new ClientDTO();
        dto.setClientId("client");
        dto.setRoleKey(SUPER_ADMIN);

        when(tenantContextHolder.getTenantKey()).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(roleService.getRoles(DEFAULT_TENANT_KEY_VALUE)).thenReturn(new TreeMap<>());

        when(applicationProperties.isClientAsSuperAdminEnabled()).thenReturn(true);

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clientService.createClient(dto);

        verify(clientRepository).save(any());
    }

    @Test
    void shouldRejectSuperAdminWhenFeatureDisabled() {
        ClientDTO dto = new ClientDTO();
        dto.setClientId("client");
        dto.setRoleKey(SUPER_ADMIN);

        when(tenantContextHolder.getTenantKey()).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(roleService.getRoles(DEFAULT_TENANT_KEY_VALUE)).thenReturn(new TreeMap<>());

        when(applicationProperties.isClientAsSuperAdminEnabled()).thenReturn(false);

        assertThrows(BusinessException.class, () -> clientService.createClient(dto));

        verify(clientRepository, never()).save(any());
    }

    @Test
    void shouldRejectNullRole() {
        ClientDTO dto = new ClientDTO();
        dto.setClientId("client");
        dto.setRoleKey(null);

        when(tenantContextHolder.getTenantKey()).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(roleService.getRoles(DEFAULT_TENANT_KEY_VALUE)).thenReturn(new TreeMap<>(Map.of("USER", new Role())));

        assertThrows(BusinessException.class, () -> clientService.createClient(dto));

        verify(clientRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownRoleOnUpdate() {
        Client client = new Client();
        client.setId(1L);

        ClientDTO dto = new ClientDTO();
        dto.setId(1L);
        dto.setRoleKey("UNKNOWN");

        when(tenantContextHolder.getTenantKey()).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(roleService.getRoles(DEFAULT_TENANT_KEY_VALUE)).thenReturn(new TreeMap<>(Map.of("USER", new Role())));

        assertThrows(BusinessException.class, () -> clientService.updateClient(dto));

        verify(clientRepository, never()).findById(anyLong());
    }
}
