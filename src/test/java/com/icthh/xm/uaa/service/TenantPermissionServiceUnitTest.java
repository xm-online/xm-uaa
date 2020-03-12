package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.service.dto.AccPermissionDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

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

    }

    @Test
    public void testGetRolePermissions() {

        service.onInit(XM_PERMISSIONS, readConfigFile(XM_PERMISSIONS));

        assertNotNull(service.getEnabledPermissionByRole(null));
        assertNotNull(service.getEnabledPermissionByRole(""));
        assertTrue(service.getEnabledPermissionByRole(null).isEmpty());
        assertTrue(service.getEnabledPermissionByRole("").isEmpty());

        List<AccPermissionDTO> listAmin = service.getEnabledPermissionByRole(ROLE_ADMIN);
        System.out.println("ROLE_ADMIN: ");
        listAmin.forEach(System.out::println);

        assertTrue(listAmin.stream().allMatch(p -> ROLE_ADMIN.equals(p.getRoleKey())));

        assertEquals("ACCOUNT.CREATE", listAmin.get(0).getPrivilegeKey());
        assertEquals("ACCOUNT.GET_LIST.ITEM", listAmin.get(1).getPrivilegeKey());
        assertEquals("ATTACHMENT.CREATE", listAmin.get(2).getPrivilegeKey());
        assertEquals("DASHBOARD.CREATE", listAmin.get(3).getPrivilegeKey());
        assertEquals("MISSING.PRIVILEGE", listAmin.get(4).getPrivilegeKey());

        List<AccPermissionDTO> listAnonym = service.getEnabledPermissionByRole(ROLE_ANONYMOUS);
        System.out.println("ROLE_ANONYMOUS: ");
        listAnonym.forEach(System.out::println);

        assertTrue(listAnonym.stream().allMatch(p -> ROLE_ANONYMOUS.equals(p.getRoleKey())));

        assertEquals("ACCOUNT.GET", listAnonym.get(0).getPrivilegeKey());
        assertEquals("ATTACHMENT.CREATE", listAnonym.get(1).getPrivilegeKey());
        assertEquals("DASHBOARD.GET_LIST.ITEM", listAnonym.get(2).getPrivilegeKey());

    }

    @Test
    public void testRolePermissionsRemoving() {

        service.onInit(XM_PERMISSIONS, readConfigFile(XM_PERMISSIONS));

        assertEquals(5, service.getEnabledPermissionByRole(ROLE_ADMIN).size());

        service.onRefresh(XM_PERMISSIONS, "");

        assertTrue(service.getEnabledPermissionByRole(ROLE_ADMIN).isEmpty());

    }

}

