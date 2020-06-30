package com.icthh.xm.uaa.service.permission;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class PermissionUpdateServiceIntTest {

    @MockBean
    private DatabaseConfigurationSource databaseConfigurationSource;
    @MockBean
    private PermissionsConfigModeProvider permissionsConfigModeProvider;
    @Autowired
    private PermissionUpdateService permissionUpdateService;
    @Autowired
    private TenantContextHolder contextsHolder;
    @MockBean
    private TenantRoleService tenantRoleService;

    @Test
    public void testDeleteRemovedPrivileges() {
        //given
        String msName = "test-ms";
        String key1 = "privilege-1";
        String key2 = "privilege-2";
        String customKey = "privilege-custom";
        Privilege customPrivilege = newPrivilege(customKey);

        Set<Privilege> privilegesCommon = ImmutableSet.of(newPrivilege(key1), newPrivilege(key2));
        Set<String> privilegeKeysExpected = ImmutableSet.of(key1, key2, customKey);

        when(tenantRoleService.getCustomPrivileges()).thenReturn(
            ImmutableMap.of(
                msName, ImmutableSet.of(customPrivilege),
                "other-ms", ImmutableSet.of(newPrivilege("other"))
            )
        );

        //TenantConfigMockConfiguration provides the following tenant set: "XM", "DEMO"
        when(permissionsConfigModeProvider.getMode())
            .thenReturn(PermissionsConfigMode.DATABASE) //XM
            .thenReturn(null); //DEMO

        Set<String> tenantKeysAct = new HashSet<>();
        doAnswer(invocation -> tenantKeysAct.add(contextsHolder.getPrivilegedContext().getTenantKey()
            .orElseThrow(() -> new AssertionError("Tenant key is not present")).getValue()))
            .when(databaseConfigurationSource).deletePermissionsForRemovedPrivileges(msName, privilegeKeysExpected);

        //when
        permissionUpdateService.deleteRemovedPrivileges(msName, privilegesCommon);

        //then
        verify(databaseConfigurationSource).deletePermissionsForRemovedPrivileges(msName, privilegeKeysExpected);
        assertEquals(ImmutableSet.of("XM"), tenantKeysAct);
    }

    private Privilege newPrivilege(String key) {
        Privilege privilege = new Privilege();
        privilege.setKey(key);
        return privilege;
    }
}
