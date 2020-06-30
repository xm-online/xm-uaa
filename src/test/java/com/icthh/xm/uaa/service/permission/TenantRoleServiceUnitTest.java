package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.EnvironmentService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.PermissionDTO;
import com.icthh.xm.uaa.service.dto.RoleDTO;
import com.icthh.xm.uaa.service.dto.RoleMatrixDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Optional;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.uaa.utils.FileUtil.getSingleConfigMap;
import static com.icthh.xm.uaa.utils.FileUtil.readConfigFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *
 */
@Slf4j
public class TenantRoleServiceUnitTest {

    private static final String XM_TENANT = "XM";
    private static final String ROLES_PATH = "/api/config/tenants/{tenantName}/roles.yml";
    private static final String CUSTOM_PRIVILEGES_PATH = "/api/config/tenants/XM/custom-privileges.yml";
    private static final String PERMISSIONS_PATH = "/api/config/tenants/{tenantName}/permissions.yml";

    TenantRoleService tenantRoleService;

    @Mock
    TenantConfigRepository tenantConfigRepository;

    @Mock
    CommonConfigRepository commonConfigRepository;

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

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(XM_TENANT)));

        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(permissionProperties.getRolesSpecPath()).thenReturn("/config/tenants/{tenantName}/roles.yml");
        when(permissionProperties.getPermissionsSpecPath()).thenReturn("/config/tenants/{tenantName}/permissions.yml");
        when(permissionProperties.getPrivilegesSpecPath()).thenReturn("/config/tenants/privileges.yml");

        ConfigServiceConfigurationSource configServiceConfigurationSource = new ConfigServiceConfigurationSource(
            permissionProperties, tenantConfigRepository, tenantContextHolder, commonConfigRepository);

        PermissionsConfigModeProvider permissionsConfigModeProvider = mock(TenantConfigPermissionsConfigModeProviderImpl.class);
        when(permissionsConfigModeProvider.getMode()).thenReturn(PermissionsConfigMode.CONFIGURATION_SERVICE);
        PermissionsConfigurationProvider permissionsConfigurationProvider = new PermissionsConfigurationProvider(Collections.singletonList(
            configServiceConfigurationSource), permissionsConfigModeProvider);

        tenantRoleService = new TenantRoleService(userRepository, clientRepository, xmAuthenticationContextHolder,
            environmentService, permissionsConfigurationProvider, tenantContextHolder);
    }

    @Test
    public void testGetRoles() {

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH)).thenReturn("");
        assertTrue(tenantRoleService.getRoles().isEmpty());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH)).thenReturn(null);
        assertTrue(tenantRoleService.getRoles().isEmpty());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH)).thenReturn("---");
        assertTrue(tenantRoleService.getRoles().isEmpty());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        assertEquals("test", tenantRoleService.getRoles().get("ROLE_USER").getDescription());
        assertEquals("test2", tenantRoleService.getRoles().get(SUPER_ADMIN).getDescription());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertTrue(tenantRoleService.getRoles().isEmpty());

        verify(tenantConfigRepository, times(6)).getConfigFullPath(eq(XM_TENANT), anyString());

    }

    @Test
    public void testGetRolePermissions() {

        String roleKey = "ROLE_ADMIN";

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH)).thenReturn("");
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH)).thenReturn(null);
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH)).thenReturn("---");
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/permissions.yml"));
        assertFalse(tenantRoleService.getRolePermissions(roleKey).isEmpty());
        assertEquals("ROLE_ADMIN", tenantRoleService.getRolePermissions(roleKey).get(0).getRoleKey());
        assertEquals("ATTACHMENT.CREATE", tenantRoleService.getRolePermissions(roleKey).get(0).getPrivilegeKey());

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        assertTrue(tenantRoleService.getRolePermissions(roleKey).isEmpty());

        verify(tenantConfigRepository, times(7)).getConfigFullPath(eq(XM_TENANT), eq(PERMISSIONS_PATH));

    }

    @Test
    public void testGetRole() {

        String privilegesPath = "/config/tenants/privileges.yml";

        String roleKey = "ROLE_ADMIN";

        // no configs
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH)).thenReturn("");
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH)).thenReturn("");
        when(commonConfigRepository.getConfig(isNull(), eq(singletonList(privilegesPath))))
            .thenReturn(getSingleConfigMap(privilegesPath));
        assertFalse(tenantRoleService.getRole(roleKey).isPresent());

        // privileges.yml exists
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH)).thenReturn("");
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH)).thenReturn("");
        when(commonConfigRepository.getConfig(isNull(), eq(singletonList(privilegesPath))))
            .thenReturn(getSingleConfigMap(privilegesPath, readConfigFile("/config/tenants/privileges.yml")));
        assertFalse(tenantRoleService.getRole(roleKey).isPresent());

        // roles.yml and privileges.yml exist
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH)).thenReturn("");
        when(commonConfigRepository.getConfig(isNull(), eq(singletonList(privilegesPath))))
            .thenReturn(getSingleConfigMap(privilegesPath, readConfigFile("/config/tenants/privileges.yml")));
        Optional<RoleDTO> role = tenantRoleService.getRole(roleKey);
        log.info("ROLE DTO = {}", role);
        assertTrue(role.isPresent());
        assertEquals("ROLE_ADMIN", role.get().getRoleKey());
        assertTrue(role.get().getPermissions().stream().noneMatch(PermissionDTO::isEnabled));

        // roles, privileges and permissions exists
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/permissions.yml"));
        when(commonConfigRepository.getConfig(isNull(), eq(singletonList(privilegesPath))))
            .thenReturn(getSingleConfigMap(privilegesPath, readConfigFile("/config/tenants/privileges.yml")));
        role = tenantRoleService.getRole(roleKey);
        log.info("ROLE DTO = {}", role);
        assertTrue(role.isPresent());
        assertEquals("ROLE_ADMIN", role.get().getRoleKey());
        assertTrue(role.get().getPermissions().stream().anyMatch(p -> p.getPrivilegeKey().equals("ATTACHMENT.CREATE")
                                                                      && p.isEnabled()));
        assertTrue(role.get().getPermissions().stream().noneMatch(p -> p.getPrivilegeKey().equals("MISSING.PRIVILEGE")
                                                                      && !p.isEnabled()));

    }

    @Test
    public void testGetRoleMatrixWithoutCustomPrivileges() {
        mockPrivileges();

        RoleMatrixDTO roleMatrix = tenantRoleService.getRoleMatrix();

        verify(tenantConfigRepository).getConfigFullPath(XM_TENANT, ROLES_PATH);

        assertRoles(roleMatrix);
        assertEquals(2, roleMatrix.getPermissions().size());

        assertTrue(roleMatrix.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to create new attachment")));
        assertTrue(roleMatrix.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to delete attachment")));

    }

    @Test
    public void testGetRoleMatrixWithCustomPrivileges() {
        mockCustomPrivileges();

        RoleMatrixDTO roleMatrix = tenantRoleService.getRoleMatrix();

        verify(tenantConfigRepository).getConfigFullPath(XM_TENANT, ROLES_PATH);
        verify(tenantConfigRepository).getConfigFullPath(XM_TENANT, CUSTOM_PRIVILEGES_PATH);

        assertRoles(roleMatrix);
        assertEquals(4, roleMatrix.getPermissions().size());

        assertTrue(roleMatrix.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to create new attachment")));
        assertTrue(roleMatrix.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to delete attachment")));
        assertTrue(roleMatrix.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to get custom privilege")));
        assertTrue(roleMatrix.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to edit custom privilege")));

    }

    @Test
    public void testGetRoleByKeyWithoutCustomPrivileges() {
        mockPrivileges();

        Optional<RoleDTO> optionalRole = tenantRoleService.getRole("SUPER-ADMIN");

        verify(tenantConfigRepository).getConfigFullPath(XM_TENANT, ROLES_PATH);

        assertTrue(optionalRole.isPresent());

        RoleDTO role = optionalRole.get();

        assertEquals("SUPER-ADMIN", role.getRoleKey());
        assertEquals(2, role.getPermissions().size());
        assertTrue(role.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to create new attachment")));
        assertTrue(role.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to delete attachment")));
    }

    @Test
    public void testGetRoleByKeyWithCustomPrivileges() {
        mockCustomPrivileges();

        Optional<RoleDTO> optionalRole = tenantRoleService.getRole("ROLE_ADMIN");

        verify(tenantConfigRepository).getConfigFullPath(XM_TENANT, ROLES_PATH);
        verify(tenantConfigRepository).getConfigFullPath(XM_TENANT, CUSTOM_PRIVILEGES_PATH);

        assertTrue(optionalRole.isPresent());

        RoleDTO role = optionalRole.get();

        assertEquals("ROLE_ADMIN", role.getRoleKey());
        assertEquals(4, role.getPermissions().size());
        assertTrue(role.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to create new attachment")));
        assertTrue(role.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to delete attachment")));
        assertTrue(role.getPermissions().stream().anyMatch(permission ->
            permission.getDescription().equals("Privilege to get custom privilege")));
    }

    private void mockPrivileges() {
        String privilegesPath = "/config/tenants/privileges.yml";

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        when(commonConfigRepository.getConfig(isNull(), eq(singletonList(privilegesPath))))
            .thenReturn(getSingleConfigMap(privilegesPath, readConfigFile("/config/tenants/privileges.yml")));
    }

    private void mockCustomPrivileges() {
        mockPrivileges();
        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, CUSTOM_PRIVILEGES_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/custom-privileges.yml"));
    }

    private void assertRoles(RoleMatrixDTO roleMatrix) {
        assertEquals(3, roleMatrix.getRoles().size());
        assertTrue(roleMatrix.getRoles().stream().anyMatch(roleKey -> roleKey.equals("ROLE_ADMIN")));
        assertTrue(roleMatrix.getRoles().stream().anyMatch(roleKey -> roleKey.equals("ROLE_USER")));
        assertTrue(roleMatrix.getRoles().stream().anyMatch(roleKey -> roleKey.equals("SUPER-ADMIN")));
    }

    @Test
    public void updateRole() {

        updateRoleMoks();

        RoleDTO newRole = new RoleDTO();

        newRole.setCreatedBy("xm");
        newRole.setUpdatedBy("xm");
        newRole.setRoleKey("ROLE_ADMIN");
        newRole.setDescription("Test update existing role");
        newRole.setCreatedDate("2019-11-09T07:27:34.757Z");
        newRole.setUpdatedDate("2019-11-09T07:27:34.757Z");

        PermissionDTO newPermission = new PermissionDTO();

        newPermission.setMsName("uaa");
        newPermission.setRoleKey("ROLE_ADMIN");
        newPermission.setPrivilegeKey("ATTACHMENT.CREATE");
        newPermission.setEnabled(false);

        newRole.setPermissions(singletonList(newPermission));

        tenantRoleService.updateRole(newRole);

        verify(tenantConfigRepository).updateConfigFullPath(eq(XM_TENANT), eq(ROLES_PATH), anyString());

        verify(tenantConfigRepository)
            .updateConfigFullPath(XM_TENANT, PERMISSIONS_PATH, readConfigFile("/RoleResourceIntTest/updatedPermissions.yml"));
    }

    private void updateRoleMoks() {
        XmAuthenticationContext authenticationContext = mock(XmAuthenticationContext.class);
        when(xmAuthenticationContextHolder.getContext()).thenReturn(authenticationContext);

        when(authenticationContext.getRequiredLogin()).thenReturn(XM_TENANT);

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, PERMISSIONS_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/permissions.yml"));

        when(tenantConfigRepository.getConfigFullPath(XM_TENANT, ROLES_PATH))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
    }
}
