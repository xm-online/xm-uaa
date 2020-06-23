package com.icthh.xm.uaa.repository;

import com.google.common.collect.ImmutableSet;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.PermissionEntity;
import com.icthh.xm.uaa.domain.RoleEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertEquals;

/**
 * Created by victor on 18.06.2020.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Transactional
public class RoleRepositoryIntTest {

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TenantContextHolder tenantContextHolder;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
    }

    @Test
    public void testSave() {
        //given
        RoleEntity role = newRandomRole();

        //when
        RoleEntity saved = roleRepository.save(role);
        entityManager.flush();

        //then
        RoleEntity retrieved = roleRepository.findById(saved.getId()).get();
        assertEquals(saved, retrieved);
    }

    @Test
    public void testSaveWithPermissions() {
        //given

        RoleEntity saved = newRandomRole();
        saved.setPermissions(ImmutableSet.of(newRandomPermission(saved), newRandomPermission(saved)));

        //when
        roleRepository.save(saved);

        entityManager.flush();

        //then
        RoleEntity retrieved = roleRepository.findById(saved.getId()).get();

        assertEquals(saved, retrieved);
        assertEquals(2, retrieved.getPermissions().size());
    }

    private PermissionEntity newRandomPermission(RoleEntity role) {
        return PermissionEntity.builder()
            .role(role)
            .disabled(false)
            .msName("ms-name")
            .privilegeKey("test-privilege")
            .build();
    }

    private RoleEntity newRandomRole() {
        return RoleEntity.builder()
            .description("test-description")
            .roleKey("TEST_ROLE")
            .build();
    }
}
