package com.icthh.xm.uaa.repository;

import com.google.common.collect.ImmutableList;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.Permission;
import com.icthh.xm.uaa.domain.Role;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;

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

    @Test
    public void testSave() throws Exception {
        //given
        Role role = newRandomRole();

        //when
        Role saved = roleRepository.save(role);
        entityManager.flush();

        //then
        Role retrieved = roleRepository.findById(saved.getId()).get();
        assertEquals(saved, retrieved);
    }

    @Test
    public void testSaveWithPermissions() throws Exception {
        //given

        Role saved = newRandomRole();
        saved.setPermissions(ImmutableList.of(newRandomPermission(saved), newRandomPermission(saved)));

        //when
        roleRepository.save(saved);

        entityManager.flush();

        //then
        Role retrieved = roleRepository.findById(saved.getId()).get();

        assertEquals(saved, retrieved);
        assertEquals(2, retrieved.getPermissions().size());
    }

    @Test
    public void testSaveWithDependOn() throws Exception {
        //given
        Role roleParent = newRandomRole();
        roleRepository.save(roleParent);

        Role roleChild = newRandomRole();
        roleChild.setBasedOn(roleParent);

        //when
        roleRepository.save(roleChild);
        entityManager.flush();

        //then
        Role childRetrieved = roleRepository.findById(roleChild.getId()).get();
        assertEquals(roleParent, childRetrieved.getBasedOn());
    }

    private Permission newRandomPermission(Role role) {//todo V: extend test data
        return Permission.builder()
            .role(role)
            .disabled(false)
            .description("test-description")
            .msName("ms-name")
            .privilegeKey("test-privilege")
            .build();
    }

    private Role newRandomRole() {
        return Role.builder()
            .description("test-description")
            .roleKey("TEST_ROLE")
            .build();
    }
}
