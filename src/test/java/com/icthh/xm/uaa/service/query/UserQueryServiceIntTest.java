package com.icthh.xm.uaa.service.query;

import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.query.filter.SoftUserFilterQuery;
import com.icthh.xm.uaa.service.query.filter.StrictUserFilterQuery;
import io.github.jhipster.service.filter.BooleanFilter;
import io.github.jhipster.service.filter.StringFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.icthh.xm.uaa.utils.FileUtil.readConfigFile;
import static java.lang.Boolean.TRUE;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserQueryService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class UserQueryServiceIntTest {

    private static final String UAA_CONFIG_PATH = "/config/tenants/XM/uaa/uaa.yml";

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_B2B_MANAGER = "ROLE_B2B_MANAGER";

    private static final String FIRST_USER_KEY = "strictFirst";
    private static final String SECOND_USER_KEY = "strictSecond";

    private static final String FIRST_USER_LOGIN = "firstTest@gmail.com";
    private static final String SECOND_USER_LOGIN = "secondTest@gmail.com";

    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQueryService userQueryService;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Before
    public void before() {
        tenantPropertiesService.onInit(UAA_CONFIG_PATH, readConfigFile(UAA_CONFIG_PATH));

        userRepository.deleteAll();
        createTestUser(FIRST_USER_KEY, FIRST_USER_LOGIN);
        createTestUser(SECOND_USER_KEY, SECOND_USER_LOGIN);
    }

    @Test
    public void findAllUsersByStrictMatch_returnOneOfList() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setLogin(new StringFilter().setContains("Test"));
        filterQuery.setRoleKey(new StringFilter().setContains(ROLE_USER));
        filterQuery.setActivated(new BooleanFilter());

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 1));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_equals() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setLogin((StringFilter) new StringFilter().setEquals(FIRST_USER_LOGIN));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_valueIn() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setLogin((StringFilter) new StringFilter().setIn(List.of(FIRST_USER_LOGIN, SECOND_USER_LOGIN)));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(2, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_notContains() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setLogin(new StringFilter().setDoesNotContain("first"));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_notEquals() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setLogin((StringFilter) new StringFilter().setNotEquals(FIRST_USER_LOGIN));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_specified() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setLogin((StringFilter) new StringFilter().setSpecified(TRUE));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(2, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_filterValueIsNull() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setLogin(new StringFilter().setContains(null));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(2, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_returnAllByFilterAuthority() {
        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();

        filterQuery.setLogin(new StringFilter().setContains("Test"));
        filterQuery.setAuthority(new StringFilter().setContains(ROLE_B2B_MANAGER));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 1));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
        Assert.assertTrue(page.getContent().get(0).getAuthorities().contains(ROLE_B2B_MANAGER));
    }

    @Test
    public void findAllUsersBySoftMatch() {

        SoftUserFilterQuery filterQuery = new SoftUserFilterQuery();
        filterQuery.setQuery(new StringFilter().setContains(FIRST_NAME));

        Page<UserDTO> page = userQueryService.findAllUsersBySoftMatch(filterQuery, PageRequest.of(0, 1));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    private void createTestUser(String userKey, String email) {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin(email);

        User user = new User();
        user.setUserKey(userKey);
        user.setAuthorities(List.of(ROLE_USER, ROLE_B2B_MANAGER));
        user.setPassword(RandomStringUtils.random(60));
        user.setPasswordSetByUser(true);
        user.setActivated(true);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);

        userRepository.saveAndFlush(user);
    }
}
