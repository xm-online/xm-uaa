package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.service.PermissionMappingService;
import com.icthh.xm.commons.permission.service.filter.EqualsOrNullPermissionMsNameFilter;
import com.icthh.xm.commons.permission.service.filter.PermissionMsNameFilter;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.service.dto.AccPermissionDTO;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import static com.icthh.xm.uaa.utils.FileUtil.readConfigFile;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TenantPermissionServiceUnitTest {

    private static final String TENANT = "XM";

    private static final String XM_PERMISSIONS = "/config/tenants/XM/permissions_serviceTest.yml";

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

    @InjectMocks
    TenantPermissionService service;

    @Mock
    PermissionProperties permissionProperties;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Spy
    private PermissionMsNameFilter filter = new EqualsOrNullPermissionMsNameFilter();

    @Spy
    private PermissionMappingService permissionMappingService = new PermissionMappingService(filter);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void before() {

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));

        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(permissionProperties.getPermissionsSpecPath()).thenReturn(
            "/config/tenants/{tenantName}/permissions_serviceTest.yml");

        ReflectionTestUtils.setField(filter, "msName", "uaa", String.class);
    }

    @Test
    public void testGetRolePermissions() {

        service.onInit(XM_PERMISSIONS, readConfigFile(XM_PERMISSIONS));

        assertNotNull(service.getEnabledPermissionByRole(null));
        assertNotNull(service.getEnabledPermissionByRole(List.of("")));
        assertTrue(service.getEnabledPermissionByRole(null).isEmpty());
        assertTrue(service.getEnabledPermissionByRole(List.of("")).isEmpty());

        List<AccPermissionDTO> listAmin = service.getEnabledPermissionByRole(List.of(ROLE_ADMIN));
        System.out.println("ROLE_ADMIN: ");
        listAmin.forEach(System.out::println);

        assertTrue(listAmin.stream().allMatch(p -> ROLE_ADMIN.equals(p.getRoleKey())));

        List<String> privileges = listAmin.stream().map(AccPermissionDTO::getPrivilegeKey).collect(Collectors.toList());
        assertTrue(privileges.contains("ACCOUNT.CREATE"));
        assertTrue(privileges.contains("ACCOUNT.GET_LIST.ITEM"));
        assertTrue(privileges.contains("ATTACHMENT.CREATE"));
        assertTrue(privileges.contains("DASHBOARD.CREATE"));
        assertTrue(privileges.contains("MISSING.PRIVILEGE"));

        List<AccPermissionDTO> listAnonym = service.getEnabledPermissionByRole(List.of(ROLE_ANONYMOUS));
        privileges = listAnonym.stream().map(AccPermissionDTO::getPrivilegeKey).collect(Collectors.toList());
        System.out.println("ROLE_ANONYMOUS: ");
        listAnonym.forEach(System.out::println);

        assertTrue(listAnonym.stream().allMatch(p -> ROLE_ANONYMOUS.equals(p.getRoleKey())));

        assertTrue(privileges.contains("ACCOUNT.GET"));
        assertTrue(privileges.contains("ATTACHMENT.CREATE"));
        assertTrue(privileges.contains("DASHBOARD.GET_LIST.ITEM"));

    }

    @Test
    public void testRolePermissionsRemoving() {

        service.onInit(XM_PERMISSIONS, readConfigFile(XM_PERMISSIONS));

        assertEquals(5, service.getEnabledPermissionByRole(List.of(ROLE_ADMIN)).size());

        service.onRefresh(XM_PERMISSIONS, "");

        assertTrue(service.getEnabledPermissionByRole(List.of(ROLE_ADMIN)).isEmpty());

    }

}

