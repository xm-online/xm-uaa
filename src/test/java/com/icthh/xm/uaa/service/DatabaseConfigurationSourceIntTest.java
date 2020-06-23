package com.icthh.xm.uaa.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by victor on 19.06.2020.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Transactional
public class DatabaseConfigurationSourceIntTest {//todo V!: check Hibernate queries, review and extend tests

    @Autowired
    DatabaseConfigurationSource databaseConfigurationSource;

    @Autowired
    TenantContextHolder tenantContextHolder;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
    }

    @Test
    public void testAddRolesAndGet() {
        //given
        Role role1 = newRandomRole();
        Role role2 = newRandomRole();

        ImmutableMap<String, Role> rolesExpected = ImmutableMap.of(
            role1.getKey(), role1,
            role2.getKey(), role2
        );

        //when
        databaseConfigurationSource.updateRoles(rolesExpected);

        //then
        Map<String, Role> rolesAct = databaseConfigurationSource.getRoles();
        Assert.assertNotNull(rolesAct);
        assertEquals(2, rolesAct.size());
        assertEquals(rolesExpected, rolesAct);
    }

    @Test
    public void testUpdate() {
        //given
        Role role1 = newRandomRole();
        Role role2 = newRandomRole();

        databaseConfigurationSource.updateRoles(ImmutableMap.of(
            role1.getKey(), role1,
            role2.getKey(), role2
        ));

        Role role3 = newRandomRole();

        //role 1 - delete
        //role 2 - update
        //role 3 - add

        role2.setDescription(randParameter("updated-description"));

        ImmutableMap<String, Role> rolesExpected = ImmutableMap.of(
            role3.getKey(), role3,
            role2.getKey(), role2
        );

        //when
        databaseConfigurationSource.updateRoles(rolesExpected);

        //then
        Map<String, Role> rolesAct = databaseConfigurationSource.getRoles();
        assertEquals(rolesAct, rolesExpected);
    }

    @Test
    public void testAddPermissions() {
        //given
        Role role1 = newRandomRole();
        Role role2 = newRandomRole();

        databaseConfigurationSource.updateRoles(ImmutableMap.of(
            role1.getKey(), role1,
            role2.getKey(), role2
        ));

        String ms1 = randParameter("ms1");
        String ms2 = randParameter("ms2");

        ImmutableMap<String, Map<String, Set<Permission>>> permissionsExpected = ImmutableMap.<String, Map<String, Set<Permission>>>builder()
            .put(ms1, ImmutableMap.of(
                role1.getKey(),
                ImmutableSet.of(
                    newRandomPermission(role1, ms1),
                    newRandomPermission(role1, ms1)
                ),

                role2.getKey(),
                ImmutableSet.of(
                    newRandomPermission(role2, ms1),
                    newRandomPermission(role2, ms1)
                )
            ))
            .put(ms2, ImmutableMap.of(
                role2.getKey(),
                ImmutableSet.of(
                    newRandomPermission(role2, ms2),
                    newRandomPermission(role2, ms2)
                )
            ))
            .build();


        //when
        databaseConfigurationSource.updatePermissions(permissionsExpected);

        //then
        Map<String, Map<String, Set<Permission>>> permissionsAct =
            databaseConfigurationSource.getPermissions();
        assertEquals(permissionsExpected, permissionsAct);
    }

    @Test
    public void testUpdateRoles() {
        //given
        //given
        Role role1 = newRandomRole();
        Role role2 = newRandomRole();

        databaseConfigurationSource.updateRoles(ImmutableMap.of(
            role1.getKey(), role1,
            role2.getKey(), role2
        ));

        String ms1 = randParameter("ms1");
        String ms2 = randParameter("ms2");


        Permission updated = newRandomPermission(role1, ms1);

        databaseConfigurationSource.updatePermissions(ImmutableMap.<String, Map<String, Set<Permission>>>builder()
            .put(ms1, ImmutableMap.of(
                role1.getKey(),
                ImmutableSet.of(
                    updated,
                    newRandomPermission(role1, ms1)
                ),

                role2.getKey(),
                ImmutableSet.of(
                    newRandomPermission(role2, ms1),
                    newRandomPermission(role2, ms1)
                )
            ))
            .put(ms2, ImmutableMap.of(
                role2.getKey(),
                ImmutableSet.of(
                    newRandomPermission(role2, ms2),
                    newRandomPermission(role2, ms2)
                )
            ))
            .build());

        updated.setPrivilegeKey(randParameter("updated-key"));
        ImmutableMap<String, Map<String, Set<Permission>>> expected = ImmutableMap.<String, Map<String, Set<Permission>>>builder()
            .put(ms1, ImmutableMap.of(
                role1.getKey(),
                ImmutableSet.of(
                    updated,
                    newRandomPermission(role1, ms1)
                ),

                role2.getKey(),
                ImmutableSet.of(
                    newRandomPermission(role2, ms1),
                    newRandomPermission(role2, ms1)
                )
            ))
            .put(ms2, ImmutableMap.of(
                role2.getKey(),
                ImmutableSet.of(
                    newRandomPermission(role2, ms2),
                    newRandomPermission(role2, ms2)
                )
            ))
            .build();
        //when
        databaseConfigurationSource.updatePermissions(expected);

        //then
        Map<String, Map<String, Set<Permission>>> permissionsAct =
            databaseConfigurationSource.getPermissions();

        assertEquals(expected, permissionsAct);
    }

    private Permission newRandomPermission(Role role, String ms) {
        Permission permission = new Permission();
        permission.setRoleKey(role.getKey());
        permission.setMsName(ms);
        permission.setPrivilegeKey(randParameter("privilege-key"));
        //todo V!: add expressions
        return permission;
    }

    private Role newRandomRole() {
        Role role = new Role();
        role.setKey(randParameter("key"));
        role.setDescription(randParameter("description"));
        return role;
    }

    public String randParameter(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }

}
