package com.icthh.xm.uaa.service.query;

import com.icthh.xm.commons.migration.db.jsonb.JsonbExpression;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria;
import com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.Operation;
import com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.ValueType;
import com.icthh.xm.uaa.service.query.filter.StrictUserFilterQuery;
import io.github.jhipster.service.filter.StringFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.Operation.CONTAINS;
import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.Operation.EQUALS;
import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.ValueType.BOOLEAN;
import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.ValueType.NUMBER;
import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.ValueType.STRING;
import static com.icthh.xm.uaa.utils.FileUtil.readConfigFile;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.getField;

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
@ContextConfiguration(initializers = {UserQueryServicePostgresIntTest.Initializer.class})
@Slf4j
@ActiveProfiles("pg-test")
public class UserQueryServicePostgresIntTest {

    private static final String UAA_CONFIG_PATH = "/config/tenants/XM/uaa/uaa.yml";

    private static final String CUSTOM_EXPRESSION_FIELD = "customExpression";

    private static final String ROLE_USER = "ROLE_SALESMAN";
    private static final String ROLE_B2B_MANAGER = "ROLE_B2B_MANAGER";

    private static final String FIRST_USER_KEY = "strictFirst";
    private static final String SECOND_USER_KEY = "strictSecond";
    private static final String THIRD_USER_KEY = "strictThird";

    private static final String FIRST_USER_LOGIN = "firstTest@gmail.com";
    private static final String SECOND_USER_LOGIN = "secondTest@gmail.com";
    private static final String THIRD_USER_LOGIN = "thirdTest@gmail.com";

    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    private static final String FIRST_KEY = "firstKey";
    private static final String SECOND_KEY = "secondKey";
    private static final String THIRD_KEY = "thirdKey";
    private static final String FIRST_VALUE_STRING = "firstValue";
    private static final String PART_FIRST_VALUE_STRING = "first";
    private static final Double SECOND_VALUE_NUMBER = 888.1D;
    private static final Boolean THIRD_VALUE_BOOLEAN = true;

    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:14.17")
         .withDatabaseName("uaa")
         .withUsername("xm")
         .withPassword("xm");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
            log.info("spring.datasource.url: {}", postgreSQLContainer.getJdbcUrl());
        }
    }

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
        createTestUser(FIRST_USER_KEY, FIRST_USER_LOGIN, Map.of(FIRST_KEY, FIRST_VALUE_STRING));
        createTestUser(SECOND_USER_KEY, SECOND_USER_LOGIN, Map.of(SECOND_KEY, SECOND_VALUE_NUMBER));
        createTestUser(THIRD_USER_KEY, THIRD_USER_LOGIN, Map.of(THIRD_KEY, THIRD_VALUE_BOOLEAN));
    }

    @Test
    public void checkPostgresExpression() {
        Object customExpression = getField(userQueryService, CUSTOM_EXPRESSION_FIELD);

        assertTrue(customExpression instanceof JsonbExpression);
    }

    @Test
    public void findAllUsersByStringDataValue_equals() {
        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setRoleKey(new StringFilter().setContains(ROLE_USER));
        filterQuery.setAuthority(new StringFilter().setContains(ROLE_USER));
        List<DataAttributeCriteria> dataAttributesCriteria = buildDataAttributesCriteria(FIRST_KEY, FIRST_VALUE_STRING, STRING, EQUALS);
        filterQuery.setDataAttributes(dataAttributesCriteria);

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
        Assert.assertEquals(FIRST_USER_KEY, page.getContent().get(0).getUserKey());
    }

    @Test
    public void findAllUsersByStringDataValue_contains() {
        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setRoleKey(new StringFilter().setContains(ROLE_USER));
        filterQuery.setAuthority(new StringFilter().setContains(ROLE_USER));
        List<DataAttributeCriteria> dataAttributesCriteria = buildDataAttributesCriteria(FIRST_KEY, PART_FIRST_VALUE_STRING, STRING, CONTAINS);

        filterQuery.setDataAttributes(dataAttributesCriteria);

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
        Assert.assertEquals(FIRST_USER_KEY, page.getContent().get(0).getUserKey());
    }

    @Test
    public void findAllUsersByNumberDataValue_equals() {
        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setRoleKey(new StringFilter().setContains(ROLE_USER));
        filterQuery.setAuthority(new StringFilter().setContains(ROLE_USER));
        List<DataAttributeCriteria> dataAttributesCriteria = buildDataAttributesCriteria(SECOND_KEY, SECOND_VALUE_NUMBER.toString(), NUMBER, EQUALS);

        filterQuery.setDataAttributes(dataAttributesCriteria);

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
        Assert.assertEquals(SECOND_USER_KEY, page.getContent().get(0).getUserKey());
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    public void findAllUsersByNumberDataValue_incompatibleValueWithValueType() {
        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setRoleKey(new StringFilter().setContains(ROLE_USER));
        filterQuery.setAuthority(new StringFilter().setContains(ROLE_USER));
        List<DataAttributeCriteria> dataAttributesCriteria = buildDataAttributesCriteria(SECOND_KEY, FIRST_VALUE_STRING, NUMBER, EQUALS);

        filterQuery.setDataAttributes(dataAttributesCriteria);

        userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));
    }

    @Test
    public void findAllUsersByBooleanDataValue_equals() {
        StrictUserFilterQuery filterQuery = new StrictUserFilterQuery();
        filterQuery.setRoleKey(new StringFilter().setContains(ROLE_USER));
        filterQuery.setAuthority(new StringFilter().setContains(ROLE_USER));
        List<DataAttributeCriteria> dataAttributesCriteria = buildDataAttributesCriteria(THIRD_KEY, THIRD_VALUE_BOOLEAN.toString(), BOOLEAN, EQUALS);

        filterQuery.setDataAttributes(dataAttributesCriteria);

        Page<UserDTO> page = userQueryService.findAllUsersByStrictMatch(filterQuery, PageRequest.of(0, 3));

        Assert.assertEquals(1, page.getTotalElements());
        Assert.assertEquals(1, page.getContent().size());
        Assert.assertEquals(THIRD_USER_KEY, page.getContent().get(0).getUserKey());
    }

    private static List<DataAttributeCriteria> buildDataAttributesCriteria(String path, String value, ValueType type, Operation operation) {
        return List.of(new DataAttributeCriteria().toBuilder()
            .path(path)
            .value(value)
            .operation(operation)
            .type(type)
            .build()
        );
    }

    private void createTestUser(String userKey, String email, Map<String, Object> data) {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin(email);

        User user = new User();
        user.setUserKey(userKey);
        user.setAuthorities(List.of(ROLE_USER, ROLE_B2B_MANAGER));
        String random = RandomStringUtils.random(60);
        user.setPassword(random);
        user.setPasswordSetByUser(true);
        user.setActivated(true);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setData(data);

        userRepository.saveAndFlush(user);
    }
}
