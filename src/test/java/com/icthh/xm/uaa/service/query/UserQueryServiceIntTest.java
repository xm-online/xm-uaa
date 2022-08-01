package com.icthh.xm.uaa.service.query;

import com.icthh.xm.commons.tenant.TenantContextHolder;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
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
@Transactional
public class UserQueryServiceIntTest {

    private static final String ROLE_SUPER_ADMIN = "SUPER-ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

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
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void before() {
        tenantPropertiesService.onInit("/config/tenants/XM/uaa/uaa.yml", "{}");
        createTestUser(FIRST_USER_KEY, FIRST_USER_LOGIN);
        createTestUser(SECOND_USER_KEY, SECOND_USER_LOGIN);
    }

    @BeforeTransaction
    public void beforeTransaction() {
        tenantContextHolder.getPrivilegedContext().setTenant(buildTenant(DEFAULT_TENANT_KEY_VALUE));
    }

    @AfterTransaction
    public void afterTransaction() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
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

        StrictUserFilterQuery adminFilterQuery = new StrictUserFilterQuery();
        adminFilterQuery.setLogin((StringFilter) new StringFilter().setIn(List.of(FIRST_USER_LOGIN, SECOND_USER_LOGIN)));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(adminFilterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(2, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_notContains() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setRoleKey((StringFilter) new StringFilter().setNotEquals(ROLE_SUPER_ADMIN));
        filterQuery.setLogin(new StringFilter().setDoesNotContain("first"));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_notEquals() {

        StrictUserFilterQuery adminFilterQuery = new StrictUserFilterQuery();
        adminFilterQuery.setRoleKey((StringFilter) new StringFilter().setNotEquals(ROLE_SUPER_ADMIN));
        adminFilterQuery.setLogin((StringFilter) new StringFilter().setNotEquals(FIRST_USER_LOGIN));

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(adminFilterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllUsersByStrictMatch_specified() {

        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setRoleKey((StringFilter) new StringFilter().setNotEquals(ROLE_SUPER_ADMIN));
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

        Assert.assertEquals(3, page.getTotalElements());
        Assert.assertEquals(3, page.getContent().size());
    }

    @Test
    public void findAllUsersBySoftMatch() {

        SoftUserFilterQuery filterQuery = new SoftUserFilterQuery();
        filterQuery.setQuery(new StringFilter().setContains(FIRST_NAME));

        Page<UserDTO> page = userQueryService.findAllUsersBySoftMatch(filterQuery, PageRequest.of(0, 1));

        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
    }

    private User createTestUser(String userKey, String email) {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin(email);

        User user = new User();
        user.setUserKey(userKey);
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);

        return userRepository.saveAndFlush(user);
    }
}
