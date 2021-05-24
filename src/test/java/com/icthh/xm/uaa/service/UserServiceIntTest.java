package com.icthh.xm.uaa.service;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings.PasswordSettings;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.util.RandomUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Transactional
public class UserServiceIntTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeTransaction
    public void beforeTransaction() {
        tenantContextHolder.getPrivilegedContext().setTenant(buildTenant(DEFAULT_TENANT_KEY_VALUE));
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @AfterTransaction
    public void afterTransaction() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Before
    public void before() {
        tenantPropertiesService.onInit("/config/tenants/XM/uaa/uaa.yml", "{}");
    }

    private static final String ROLE_USER = "ROLE_USER";

    @Test
    public void assertThatUserMustExistToResetPassword() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("admin@localhost");

        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        userRepository.saveAndFlush(user);

        Optional<User> maybeUser = userService.requestPasswordReset("john.doe@localhost");
        assertThat(maybeUser.isPresent()).isFalse();

        maybeUser = userService.requestPasswordReset("admin@localhost");
        assertThat(maybeUser.isPresent()).isTrue();

        assertThat(maybeUser.orElse(null).getEmail()).isEqualTo("admin@localhost");
        assertThat(maybeUser.orElse(null).getResetDate()).isNotNull();
        assertThat(maybeUser.orElse(null).getResetKey()).isNotNull();
    }

    @Test
    public void assertThatOnlyActivatedUserCanRequestPasswordReset() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("admin@localhost");

        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(false);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        userRepository.saveAndFlush(user);
        Optional<User> maybeUser = userService.requestPasswordReset("john.doe@localhost");
        assertThat(maybeUser.isPresent()).isFalse();
        userRepository.delete(user);
    }

    @Test(expected = BusinessException.class)
    public void assertThatResetKeyMustNotBeOlderThan24Hours() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("admin@localhost");

        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);

        userRepository.save(user);

        userService.completePasswordReset("johndoe2", user.getResetKey());

        userRepository.delete(user);
    }

    @Test(expected = BusinessException.class)
    public void assertThatResetKeyMustBeValid() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("admin@localhost");

        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey("1234");
        userRepository.save(user);
        userService.completePasswordReset("johndoe2", user.getResetKey());

        userRepository.delete(user);
    }

    @Test
    public void assertThatUserCanResetPassword() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("admin@localhost");

        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        String oldPassword = user.getPassword();
        Instant daysAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);
        userRepository.save(user);

        assertNull(user.getUpdatePasswordDate());

        userService.completePasswordReset("johndoe2", user.getResetKey());

        assertThat(user.getResetDate()).isNull();
        assertThat(user.getResetKey()).isNull();
        assertThat(user.getPassword()).isNotEqualTo(oldPassword);

        assertNotNull(user.getUpdatePasswordDate());

        userRepository.delete(user);
    }

    @SneakyThrows
    @Test
    public void passwordValidationSuccessTest() {
        ObjectMapper objectMapper = new ObjectMapper();
        TenantProperties tenantProperties = new TenantProperties();
        PublicSettings publicSettings = new PublicSettings();
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setEnableBackEndValidation(true);
        passwordSettings.setMinLength((byte) 4);
        passwordSettings.setMaxLength((byte) 10);
        passwordSettings.setPattern("^.{3,11}$");
        publicSettings.setPasswordSettings(passwordSettings);
        tenantProperties.setPublicSettings(publicSettings);

        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            objectMapper.writeValueAsString(tenantProperties));

        userService.validatePassword("password");
    }

    @SneakyThrows
    @Test
    public void passwordValidationMinLengthTest() {
        exception.expect(BusinessException.class);
        exception.expectMessage("password length is less than the minimum");

        ObjectMapper objectMapper = new ObjectMapper();
        TenantProperties tenantProperties = new TenantProperties();
        PublicSettings publicSettings = new PublicSettings();
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setEnableBackEndValidation(true);
        passwordSettings.setMinLength((byte) 9);
        publicSettings.setPasswordSettings(passwordSettings);
        tenantProperties.setPublicSettings(publicSettings);

        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            objectMapper.writeValueAsString(tenantProperties));

        assertThatCode(() -> userService.validatePassword("password1")).doesNotThrowAnyException();
        userService.validatePassword("password");
    }

    @SneakyThrows
    @Test
    public void passwordValidationMaxLengthTest() {
        exception.expect(BusinessException.class);
        exception.expectMessage("password length is greater than the maximum");

        ObjectMapper objectMapper = new ObjectMapper();
        TenantProperties tenantProperties = new TenantProperties();
        PublicSettings publicSettings = new PublicSettings();
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setEnableBackEndValidation(true);
        passwordSettings.setMaxLength((byte) 7);
        publicSettings.setPasswordSettings(passwordSettings);
        tenantProperties.setPublicSettings(publicSettings);

        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            objectMapper.writeValueAsString(tenantProperties));

        assertThatCode(() -> userService.validatePassword("passwor")).doesNotThrowAnyException();
        userService.validatePassword("password");

    }

    @SneakyThrows
    @Test
    public void passwordValidationPatternTest() {
        exception.expect(BusinessException.class);
        exception.expectMessage("password doesn't match regex");

        ObjectMapper objectMapper = new ObjectMapper();
        TenantProperties tenantProperties = new TenantProperties();
        PublicSettings publicSettings = new PublicSettings();
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setEnableBackEndValidation(true);
        passwordSettings.setPattern("((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!#$]).{6,15})");
        publicSettings.setPasswordSettings(passwordSettings);
        tenantProperties.setPublicSettings(publicSettings);

        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            objectMapper.writeValueAsString(tenantProperties));

        assertThatCode(() -> userService.validatePassword("Password5!")).doesNotThrowAnyException();
        userService.validatePassword("password");
    }


    @SneakyThrows
    @Test
    public void passwordValidationPoliciesTest() {
        exception.expect(BusinessException.class);
        exception.expectMessage("password doesn't matched required count of policies");

        validateByTwoRegexp(3L);
    }

    @SneakyThrows
    @Test
    public void passwordValidationPoliciesMinimalCountTest() {
        validateByTwoRegexp(2L);
    }

    private void validateByTwoRegexp(long passwordPoliciesMinimalMatchCount) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TenantProperties tenantProperties = new TenantProperties();
        PublicSettings publicSettings = new PublicSettings();
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setEnableBackEndValidation(true);
        passwordSettings.setPattern("((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!#$]).{6,15})");

        List<PublicSettings.PasswordPolicy> policyList = new ArrayList<>();
        policyList.add(initPasswordPolicy(".*.*.*"));
        policyList.add(initPasswordPolicy(".*\\d.*"));
        policyList.add(initPasswordPolicy("\\\\+\\d+$"));
        publicSettings.setPasswordPoliciesMinimalMatchCount(passwordPoliciesMinimalMatchCount);

        publicSettings.setPasswordPolicies(policyList);
        publicSettings.setPasswordSettings(passwordSettings);
        tenantProperties.setPublicSettings(publicSettings);

        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            objectMapper.writeValueAsString(tenantProperties));

        userService.validatePassword("Password5!");
    }

    private PublicSettings.PasswordPolicy initPasswordPolicy(String pattern){
        PublicSettings.PasswordPolicy passwordPolicy  = new PublicSettings.PasswordPolicy();
        passwordPolicy.setPattern(pattern);
        return passwordPolicy;
    }
}
