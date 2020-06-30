package com.icthh.xm.uaa.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Transactional
public class DatabaseConfigurationSourceIntTest {//todo V: check Hibernate queries

    @Autowired
    DatabaseConfigurationSource databaseConfigurationSource;

    @Autowired
    TenantContextHolder tenantContextHolder;

    @Autowired
    EntityManager entityManager;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
    }

    @Test
    public void testAddSingleRole() {
        //given
        String description = randParameter("description");
        String key = randParameter("key");

        //role.key must be set from rolesExpected
        Role role = new Role();
        role.setDescription(description);

        Map<String, Role> rolesExpected = ImmutableMap.of(key, role);

        //when
        databaseConfigurationSource.updateRoles(rolesExpected);
        entityManager.flush();

        //then
        Map<String, Role> rolesAct = databaseConfigurationSource.getRoles();
        Assert.assertNotNull(rolesAct);
        assertEquals(1, rolesAct.size());
        Role roleAct = rolesAct.get(key);
        assertNotNull(roleAct);
        assertEquals(roleAct.getKey(), key);
        assertEquals(roleAct.getDescription(), description);
    }

    @Test
    public void testUpdateSingleRole() {
        //given
        String description = randParameter("description");
        String key = randParameter("key");

        Role role = new Role();
        role.setDescription(randParameter("description"));

        Map<String, Role> rolesExpected = ImmutableMap.of(key, role);
        databaseConfigurationSource.updateRoles(rolesExpected);
        entityManager.flush();

        role.setDescription(description);

        //when
        databaseConfigurationSource.updateRoles(rolesExpected);
        entityManager.flush();

        //then
        Map<String, Role> rolesAct = databaseConfigurationSource.getRoles();
        Assert.assertNotNull(rolesAct);
        assertEquals(1, rolesAct.size());
        Role roleAct = rolesAct.get(key);
        assertNotNull(roleAct);
        assertEquals(roleAct.getKey(), key);
        assertEquals(roleAct.getDescription(), description);
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
        entityManager.flush();

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
        entityManager.flush();

        //then
        Map<String, Role> rolesAct = databaseConfigurationSource.getRoles();
        assertEquals(rolesAct, rolesExpected);
    }

    @Test
    public void testAddSinglePermission() {
        //given
        Role role = newRandomRole();
        databaseConfigurationSource.updateRoles(ImmutableMap.of(
            role.getKey(), role
        ));

        String msName = randParameter("ms-name");
        String privilegeKey = randParameter("privilege-key");
        Expression resourceCondition = newCondition();
        Expression envCondition = newCondition();

        Permission permission = new Permission();
        permission.setRoleKey(role.getKey());
        permission.setPrivilegeKey(privilegeKey);
        permission.setResourceCondition(resourceCondition);
        permission.setEnvCondition(envCondition);

        //when
        databaseConfigurationSource.updatePermissions(ImmutableMap.of(
            msName, ImmutableMap.of(
                role.getKey(),
                ImmutableSet.of(permission))));
        entityManager.flush();

        //then
        Map<String, Map<String, Set<Permission>>> result =
            databaseConfigurationSource.getPermissions();

        assertEquals(1, result.size());
        Map<String, Set<Permission>> actRolePermissions = result.get(msName);
        assertNotNull(actRolePermissions);
        assertEquals(1, actRolePermissions.size());
        Set<Permission> permissionsAct = actRolePermissions.get(role.getKey());
        assertEquals(1, permissionsAct.size());
        Permission permissionAct = permissionsAct.iterator().next();

        assertEquals(permissionAct.getMsName(), msName);
        assertEquals(permissionAct.getPrivilegeKey(), privilegeKey);
        assertEquals(permissionAct.getEnvCondition().getExpressionString(), envCondition.getExpressionString());
        assertEquals(permissionAct.getResourceCondition().getExpressionString(), resourceCondition.getExpressionString());
    }

    @Test
    public void testUpdateSinglePermission() {
        //given
        Role role = newRandomRole();
        databaseConfigurationSource.updateRoles(ImmutableMap.of(
            role.getKey(), role
        ));

        String msName = randParameter("ms-name");
        String privilegeKey = randParameter("privilege-key");
        Expression resourceCondition = newCondition();
        Expression envCondition = newCondition();

        Permission permission = new Permission();
        permission.setRoleKey(role.getKey());
        permission.setPrivilegeKey(privilegeKey);
        permission.setResourceCondition(resourceCondition);
        permission.setEnvCondition(envCondition);
        permission.setDisabled(true);

        databaseConfigurationSource.updatePermissions(ImmutableMap.of(
            msName, ImmutableMap.of(
                role.getKey(),
                ImmutableSet.of(permission))));
        entityManager.flush();

        permission.setDisabled(false);

        //when
        databaseConfigurationSource.updatePermissions(ImmutableMap.of(
            msName, ImmutableMap.of(
                role.getKey(),
                ImmutableSet.of(permission))));
        entityManager.flush();

        //then
        Map<String, Map<String, Set<Permission>>> result =
            databaseConfigurationSource.getPermissions();

        assertEquals(1, result.size());
        Map<String, Set<Permission>> actRolePermissions = result.get(msName);
        assertNotNull(actRolePermissions);
        assertEquals(1, actRolePermissions.size());
        Set<Permission> permissionsAct = actRolePermissions.get(role.getKey());
        assertEquals(1, permissionsAct.size());
        Permission permissionAct = permissionsAct.iterator().next();

        assertEquals(permissionAct.getMsName(), msName);
        assertEquals(permissionAct.getPrivilegeKey(), privilegeKey);
        assertEquals(permissionAct.getEnvCondition().getExpressionString(), envCondition.getExpressionString());
        assertEquals(permissionAct.getResourceCondition().getExpressionString(), resourceCondition.getExpressionString());
        assertFalse(permissionAct.isDisabled());
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
        entityManager.flush();

        //then
        Map<String, Map<String, Set<Permission>>> permissionsAct =
            databaseConfigurationSource.getPermissions();
        assertEquals(permissionsExpected, permissionsAct);
    }

    @Test
    public void testUpdateRoles() {
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
                    newRandomPermission(role2, ms1) //new permissions don't have msName
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
        entityManager.flush();

        //then
        Map<String, Map<String, Set<Permission>>> permissionsAct =
            databaseConfigurationSource.getPermissions();

        assertEquals(expected, permissionsAct);
    }

    @Test
    public void testDeleteNotIn() {
        //given
        String msName = randParameter("ms");
        String msOther = randParameter("ms-other");

        Role role = newRandomRole();
        databaseConfigurationSource.updateRoles(ImmutableMap.of(role.getKey(), role));

        Permission permissionExpected = newRandomPermission(role, msName);
        Permission permissionShouldBeDeleted = newRandomPermission(role, msName);
        Permission permissionOther = newRandomPermission(role, msOther);

        databaseConfigurationSource.updatePermissions(ImmutableMap.of(
            msName, ImmutableMap.of(
                role.getKey(), ImmutableSet.of(permissionExpected, permissionShouldBeDeleted)),
            msOther, ImmutableMap.of(
                role.getKey(), ImmutableSet.of(permissionOther)))
        );
        entityManager.flush();
        entityManager.clear();

        //when
        databaseConfigurationSource.deletePermissionsForRemovedPrivileges(msName, ImmutableList.of(permissionExpected.getPrivilegeKey()));

        //then
        Map<String, Map<String, Set<Permission>>> privilegesActual = databaseConfigurationSource.getPermissions();
        assertEquals(ImmutableMap.of(
            msName, ImmutableMap.of(
                role.getKey(),
                ImmutableSet.of(permissionExpected)),
            msOther, ImmutableMap.of(
                role.getKey(), ImmutableSet.of(permissionOther))
            ), privilegesActual
        );
    }

    @Test
    public void testDeleteByMs() {
        //given
        String msName = randParameter("ms");
        String msOther = randParameter("ms-other");

        Role role = newRandomRole();
        databaseConfigurationSource.updateRoles(ImmutableMap.of(role.getKey(), role));

        Permission permissionExisting = newRandomPermission(role, msName);
        Permission permissionOther = newRandomPermission(role, msOther);

        databaseConfigurationSource.updatePermissions(ImmutableMap.of(
            msName, ImmutableMap.of(
                role.getKey(), ImmutableSet.of(permissionExisting)),
            msOther, ImmutableMap.of(
                role.getKey(), ImmutableSet.of(permissionOther)))
        );
        entityManager.flush();
        entityManager.clear();

        //when
        databaseConfigurationSource.deletePermissionsForRemovedPrivileges(msName, Collections.emptyList());

        //then
        Map<String, Map<String, Set<Permission>>> privilegesActual = databaseConfigurationSource.getPermissions();
        assertEquals(ImmutableMap.of(
            msOther, ImmutableMap.of(
                role.getKey(), ImmutableSet.of(permissionOther))
            ), privilegesActual
        );
    }

    private Permission newRandomPermission(Role role, String ms) {
        Permission permission = new Permission();
        permission.setRoleKey(role.getKey());
        permission.setMsName(ms);
        permission.setPrivilegeKey(randParameter("privilege-key"));
        permission.setEnvCondition(newCondition());
        permission.setResourceCondition(newCondition());
        permission.setReactionStrategy(ReactionStrategy.EXCEPTION);
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

    private Expression newCondition() {
        ExpressionParser parser = new SpelExpressionParser();
        return parser.parseExpression("#returnObject.typeKey != 'USER-PROFILE'");
    }
}
