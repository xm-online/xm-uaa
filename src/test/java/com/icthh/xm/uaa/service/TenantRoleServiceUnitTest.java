package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.PermissionDTO;
import com.icthh.xm.uaa.service.dto.RoleDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
@Slf4j
public class TenantRoleServiceUnitTest {

    public static final String TENANT = "XM";

    @InjectMocks
    TenantRoleService tenantRoleService;

    @Mock
    TenantConfigRepository tenantConfigRepository;

    @Mock
    PermissionProperties permissionProperties;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantPropertiesService tenantPropertiesService;

    @Mock
    UserRepository userRepository;

    @Mock
    ClientRepository clientRepository;

    @Mock
    XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Mock
    EnvironmentService environmentService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

    }

    @Before
    public void before() {

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));

        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(permissionProperties.getRolesSpecPath()).thenReturn("/config/tenants/{tenantName}/roles.yml");
        when(permissionProperties.getPermissionsSpecPath()).thenReturn("/config/tenants/{tenantName}/permissions.yml");
        when(permissionProperties.getPrivilegesSpecPath()).thenReturn("/config/tenants/privileges.yml");

    }

    @Test
    public void testGetRoles() {

        String rolesPath = "/api/config/tenants/{tenantName}/roles.yml";

        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath)).thenReturn("");
        assertTrue(tenantRoleService.getRoles().isEmpty());

        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath)).thenReturn(null);
        assertTrue(tenantRoleService.getRoles().isEmpty());

        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath)).thenReturn("---");
        assertTrue(tenantRoleService.getRoles().isEmpty());

        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        assertEquals("test", tenantRoleService.getRoles().get("ROLE_USER").getDescription());
        assertEquals("test2", tenantRoleService.getRoles().get(SUPER_ADMIN).getDescription());

        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertTrue(tenantRoleService.getRoles().isEmpty());

        verify(tenantConfigRepository, times(6)).getConfigFullPath(eq(TENANT), anyString());

    }

    @Test
    public void testGetRolePermissions() {

        String permissionsPath = "/api/config/tenants/{tenantName}/permissions.yml";
        String roleKey = "ROLE_ADMIN";

        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath)).thenReturn("");
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath)).thenReturn(null);
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath)).thenReturn("---");
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath))
            .thenReturn(readConfigFile("/config/tenants/XM/permissions.yml"));
        assertFalse(tenantRoleService.getRolePermissions(roleKey).isEmpty());
        assertEquals("ROLE_ADMIN", tenantRoleService.getRolePermissions(roleKey).get(0).getRoleKey());
        assertEquals("ATTACHMENT.CREATE", tenantRoleService.getRolePermissions(roleKey).get(0).getPrivilegeKey());

        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        verify(tenantConfigRepository, times(7)).getConfigFullPath(eq(TENANT), eq(permissionsPath));

    }

    @Test
    public void testGetRole() {

        String privilegesPath = "/api/config/tenants/privileges.yml";
        String rolesPath = "/api/config/tenants/{tenantName}/roles.yml";
        String permissionsPath = "/api/config/tenants/{tenantName}/permissions.yml";

        String roleKey = "ROLE_ADMIN";

        // no configs
        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath)).thenReturn("");
        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath)).thenReturn("");
        when(tenantConfigRepository.getConfigFullPath(TENANT, privilegesPath)).thenReturn("");
        assertFalse(tenantRoleService.getRole(roleKey).isPresent());

        // privileges.yml exists
        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath)).thenReturn("");
        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath)).thenReturn("");
        when(tenantConfigRepository.getConfigFullPath(TENANT, privilegesPath))
            .thenReturn(readConfigFile("/config/tenants/privileges.yml"));
        assertFalse(tenantRoleService.getRole(roleKey).isPresent());

        // roles.yml and privileges.yml exist
        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath)).thenReturn("");
        when(tenantConfigRepository.getConfigFullPath(TENANT, privilegesPath))
            .thenReturn(readConfigFile("/config/tenants/privileges.yml"));
        Optional<RoleDTO> role = tenantRoleService.getRole(roleKey);
        log.info("ROLE DTO = {}", role);
        assertTrue(role.isPresent());
        assertEquals("ROLE_ADMIN", role.get().getRoleKey());
        assertTrue(role.get().getPermissions().stream().noneMatch(PermissionDTO::isEnabled));

        // roles, provileges and permittions exists
        when(tenantConfigRepository.getConfigFullPath(TENANT, rolesPath))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        when(tenantConfigRepository.getConfigFullPath(TENANT, permissionsPath))
            .thenReturn(readConfigFile("/config/tenants/XM/permissions.yml"));
        when(tenantConfigRepository.getConfigFullPath(TENANT, privilegesPath))
            .thenReturn(readConfigFile("/config/tenants/privileges.yml"));
        role = tenantRoleService.getRole(roleKey);
        log.info("ROLE DTO = {}", role);
        assertTrue(role.isPresent());
        assertEquals("ROLE_ADMIN", role.get().getRoleKey());
        assertTrue(role.get().getPermissions().stream().anyMatch(p -> p.getPrivilegeKey().equals("ATTACHMENT.CREATE")
                                                                      && p.isEnabled()));
        assertTrue(role.get().getPermissions().stream().noneMatch(p -> p.getPrivilegeKey().equals("MISSING.PRIVILEGE")
                                                                      && !p.isEnabled()));

    }

    private String readConfigFile(String path) {
        return new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(path)))
            .lines().collect(Collectors.joining("\n"));
    }

}
