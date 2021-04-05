package com.icthh.xm.uaa.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.security.XmAuthenticationConstants;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.service.AccountMailService;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.CaptchaService;
import com.icthh.xm.uaa.service.TenantPermissionService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.TenantRoleService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetHandlerFactory;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.mail.MailService;
import com.icthh.xm.uaa.web.rest.vm.ChangePasswordVM;
import com.icthh.xm.uaa.web.rest.vm.KeyAndPasswordVM;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import com.icthh.xm.uaa.web.rest.vm.ResetPasswordVM;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static com.icthh.xm.uaa.utils.FileUtil.readConfigFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the AccountResource REST controller.
 *
 * @see AccountResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class AccountResourceIntTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private HttpMessageConverter[] httpMessageConverters;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Autowired
    private LepManager lepManager;

    @Mock
    private ProfileEventProducer profileEventProducer;

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    @Mock
    private AccountMailService accountMailService;

    @Mock
    private TenantRoleService tenantRoleService;

    @Autowired
    private TenantPermissionService tenantPermissionService;

    @Mock
    private PasswordResetHandlerFactory passwordResetHandlerFactory;

    private MockMvc restUserMockMvc;

    private MockMvc restMvc;

    private static final String DEF_USER_KEY = "def_user_key";

    private static final String ROLE_USER = "ROLE_USER";

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Autowired
    private XmRequestContextHolder xmRequestContextHolder;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private UserLoginRepository userLoginRepository;


    @BeforeTransaction
    public void BeforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @Before
    @SneakyThrows
    public void setup() {
        // init tenant for test's without @Transactional
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);

        TenantProperties properties = new TenantProperties();
        TenantProperties.Security security = new TenantProperties.Security();
        security.setDefaultUserRole(ROLE_USER);

        properties.setRegistrationCaptchaPeriodSeconds(null);
        properties.setSecurity(security);
        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            new ObjectMapper(new YAMLFactory()).writeValueAsString(properties));

        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail(any(), anyString(), any(), anyString());
        doNothing().when(profileEventProducer).send(any());
        when(tenantRoleService.getRolePermissions(anyString())).thenReturn(Collections.emptyList());

        RegistrationLogRepository registrationLogRepository = mock(RegistrationLogRepository.class);
        when(registrationLogRepository.findOneByIpAddress(any())).thenReturn(Optional.empty());

        CaptchaService captchaService = new CaptchaService(applicationProperties, registrationLogRepository,
            tenantPropertiesService);

        AccountResource accountResource = new AccountResource(userRepository,
            userLoginService,
            userService,
            accountService,
            profileEventProducer,
            captchaService,
            xmAuthenticationContextHolder,
            xmRequestContextHolder,
            tenantContextHolder, tenantPermissionService, accountMailService);

        AccountResource accountUserMockResource = new AccountResource(userRepository,
            userLoginService,
            mockUserService,
            accountService,
            profileEventProducer,
            captchaService,
            xmAuthenticationContextHolder,
            xmRequestContextHolder,
            tenantContextHolder, tenantPermissionService, accountMailService);

        this.restMvc = MockMvcBuilders.standaloneSetup(accountResource)
            .setMessageConverters(httpMessageConverters).setControllerAdvice(exceptionTranslator)
            .build();
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource).build();
    }

    private void setCurrentUserKey(String userKey) {
        Map<String, String> detailsMap = new HashMap<>();
        detailsMap.put(XmAuthenticationConstants.AUTH_DETAILS_USER_KEY, userKey);

        OAuth2AuthenticationDetails authDetails = mock(OAuth2AuthenticationDetails.class);
        when(authDetails.getDecodedDetails()).thenReturn(detailsMap);

        OAuth2Authentication auth = mock(OAuth2Authentication.class);
        when(auth.getDetails()).thenReturn(authDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void executeForUserKey(String userKey, Runnable runnable) {
        Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
        setCurrentUserKey(userKey);
        try {
            runnable.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(oldAuth);
        }
    }

    @After
    public void destroy() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    public void testNonAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    public void testAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
            .with(request -> {
                request.setRemoteUser("test");
                return request;
            })
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("test"));
    }

    @Test
    public void testGetExistingAccount() {
        executeForUserKey(DEF_USER_KEY, () -> {
            UserLogin userLogin = new UserLogin();
            userLogin.setLogin("email");
            userLogin.setTypeKey(UserLoginType.EMAIL.getValue());

            User user = new User();
            user.setUserKey("test");
            user.setFirstName("john");
            user.setLastName("doe");
            user.setImageUrl("http://placehold.it/50x50");
            user.setLangKey("en");
            user.setRoleKey(RoleConstant.SUPER_ADMIN);
            user.getLogins().add(userLogin);
            when(mockUserService.findOneWithLoginsByUserKey(anyString())).thenReturn(Optional.of(user));

            try {
                restUserMockMvc.perform(get("/api/account")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(jsonPath("$.firstName").value("john"))
                    .andExpect(jsonPath("$.lastName").value("doe"))
                    .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
                    .andExpect(jsonPath("$.langKey").value("en"))
                    .andExpect(jsonPath("$.logins[0].login").value("email"))
                    .andExpect(jsonPath("$.roleKey").value(RoleConstant.SUPER_ADMIN));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    public void testGetExistingAccount_shouldReturnPermissionsFromAllApps() {
        executeForUserKey(DEF_USER_KEY, () -> {
            UserLogin userLogin = new UserLogin();
            userLogin.setLogin("email");
            userLogin.setTypeKey(UserLoginType.EMAIL.getValue());

            User user = new User();
            user.setUserKey("test");
            user.setFirstName("john");
            user.setLastName("doe");
            user.setImageUrl("http://placehold.it/50x50");
            user.setLangKey("en");
            user.setRoleKey("ROLE_ADMIN");
            user.getLogins().add(userLogin);
            when(mockUserService.findOneWithLoginsByUserKey(anyString())).thenReturn(Optional.of(user));

            tenantPermissionService.onRefresh("/config/tenants/XM/permissions.yml",
                readConfigFile("/config/tenants/XM/permissions_multiple_apps.yml"));

            try {
                restUserMockMvc.perform(get("/api/account")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(jsonPath("$.firstName").value("john"))
                    .andExpect(jsonPath("$.lastName").value("doe"))
                    .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
                    .andExpect(jsonPath("$.langKey").value("en"))
                    .andExpect(jsonPath("$.logins[0].login").value("email"))
                    .andExpect(jsonPath("$.permissions[0].msName").value("uaa"))
                    .andExpect(jsonPath("$.permissions[0].privilegeKey").value("ATTACHMENT.CREATE"))
                    .andExpect(jsonPath("$.permissions[1].msName").value("entity"))
                    .andExpect(jsonPath("$.permissions[1].privilegeKey").value("ATTACHMENT.DELETE"))
                    .andExpect(jsonPath("$.permissions[2].msName").value("uaa"))
                    .andExpect(jsonPath("$.permissions[2].privilegeKey").value("MISSING.PRIVILEGE"))
                    .andExpect(jsonPath("$.roleKey").value("ROLE_ADMIN"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    public void testGetUnknownAccount() throws Exception {
        when(mockUserService.findOneWithLoginsByUserKey(anyString())).thenReturn(Optional.empty());

        executeForUserKey(DEF_USER_KEY, () -> {
            try {
                restUserMockMvc.perform(get("/api/account")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    @Transactional
    public void testRegisterValid() throws Exception {
        UserLogin login = new UserLogin();
        login.setLogin("joe@example.com");
        login.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "password",             // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            true,                   //activated
            false, // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50",  //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            ROLE_USER, "test",
            null, null, null, null,
            Collections.singletonList(login), false, null, null,
            List.of("test"));

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<UserLogin> user = userLoginRepository.findOneByLoginIgnoreCase("joe@example.com");
        assertThat(user.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testRegisterValidWithLoginWithWhitespaces() throws Exception {
        UserLogin login = new UserLogin();
        login.setLogin("  joe@example.com   ");
        login.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "password",             // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            true,                   //activated
            false, // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50",  //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            ROLE_USER, "test",
            null, null, null,
            null, Collections.singletonList(login), false,
            null, null, List.of("test"));

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<UserLogin> user = userLoginRepository.findOneByLoginIgnoreCase("joe@example.com");
        assertThat(user.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testRegisterInvalidPassword() throws Exception {
        UserLogin login = new UserLogin();
        login.setLogin("joe@example.com");
        login.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "123",              // password with only 3 digits
            "Bob",              // firstName
            "Green",            // lastName
            true,               // activated
            false, // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            ROLE_USER, "test",
            null, null, null, null,
            Collections.singletonList(login), false, null,
            null, List.of("test"));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<UserLogin> user = userLoginRepository.findOneByLoginIgnoreCase("joe@example.com");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterNullPassword() throws Exception {
        UserLogin login = new UserLogin();
        login.setLogin("joe@example.com");
        login.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            null,               // invalid null password
            "Bob",              // firstName
            "Green",            // lastName
            true,               // activated
            false, // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            ROLE_USER, "test",
            null, null, null,
            null, Collections.singletonList(login), false, null,
            null, List.of("test"));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<UserLogin> user = userLoginRepository.findOneByLoginIgnoreCase("joe@example.com");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterDuplicateLogin() throws Exception {
        // Good
        UserLogin login = new UserLogin();
        login.setLogin("joe@example.com");
        login.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "password",             // password
            "Alice",                // firstName
            "Something",            // lastName
            true,                   // activated
            false, // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            ROLE_USER, "test",
            null, null, null, null,
            Collections.singletonList(login), false, null,
            null, List.of("test"));

        // Duplicate login, different login
        UserLogin loginNew = new UserLogin();
        loginNew.setLogin("alicejr@example.com");
        loginNew.setTypeKey(UserLoginType.NICKNAME.getValue());

        ManagedUserVM duplicatedUser = new ManagedUserVM(validUser.getId(), validUser.getPassword(),
            validUser.getFirstName(), validUser.getLastName(),
            true, false,
            null, null,
            validUser.getImageUrl(), validUser.getLangKey(), validUser.getCreatedBy(),
            validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate(),
            validUser.getAuthorities().get(0), "test",
            validUser.getAccessTokenValiditySeconds(), validUser.getRefreshTokenValiditySeconds(),
            validUser.getTfaAccessTokenValiditySeconds(),
            null, Arrays.asList(login, loginNew), false, null,
            null, validUser.getAuthorities());

        // Good user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        // Duplicate login
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andExpect(status().isBadRequest());

        Optional<UserLogin> userDup = userLoginRepository.findOneByLoginIgnoreCase("alicejr@example.com");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterDuplicateLoginWithTrailingWhitespaces() throws Exception {
        // Good
        UserLogin loginOld = new UserLogin();
        loginOld.setLogin("joe@example.com");
        loginOld.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "password",             // password
            "Alice",                // firstName
            "Something",            // lastName
            true,                   // activated
            false, // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            ROLE_USER, "test",
            null, null, null, null,
            Collections.singletonList(loginOld), false, null,
            null, List.of("test"));

        // Duplicate login, different login
        UserLogin loginNew = new UserLogin();
        loginNew.setLogin("alicejr@example.com");
        loginNew.setTypeKey(UserLoginType.NICKNAME.getValue());
        UserLogin loginOldWithWhitespaces = new UserLogin();
        loginOldWithWhitespaces.setLogin("  joe@example.com   ");
        loginOldWithWhitespaces.setTypeKey(UserLoginType.EMAIL.getValue());

        ManagedUserVM duplicatedUser = new ManagedUserVM(validUser.getId(), validUser.getPassword(),
            validUser.getFirstName(), validUser.getLastName(),
            true, false,
            null, null,
            validUser.getImageUrl(), validUser.getLangKey(), validUser.getCreatedBy(),
            validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate(),
            validUser.getAuthorities().get(0), "test",
            validUser.getAccessTokenValiditySeconds(), validUser.getRefreshTokenValiditySeconds(),
            validUser.getTfaAccessTokenValiditySeconds(),
            null, Arrays.asList(loginOldWithWhitespaces, loginNew), false,
            null, null, List.of("test"));

        // Good user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        // Duplicate login
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andExpect(status().isBadRequest());

        Optional<UserLogin> userDup = userLoginRepository.findOneByLoginIgnoreCase("alicejr@example.com");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterAdminIsIgnored() throws Exception {
        UserLogin login = new UserLogin();
        login.setLogin("joe@example.com");
        login.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "password",             // password
            "Bad",                  // firstName
            "Guy",                  // lastName
            true,                   // activated
            false,                  // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            RoleConstant.SUPER_ADMIN, "test",
            null, null, null, null,
            Collections.singletonList(login), false,
            null, null, List.of("test"));

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<UserLogin> userDup = userLoginRepository.findOneByLoginIgnoreCase("joe@example.com");
        assertThat(userDup.isPresent()).isTrue();
        assertThat(userDup.get().getUser().getAuthorities()).isEqualTo(List.of(ROLE_USER));
    }

    @Test
    @Transactional
    public void testActivateAccount() throws Exception {
        final String activationKey = "some activation key";
        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(false);
        user.setActivationKey(activationKey);

        user = userRepository.saveAndFlush(user);

        restMvc.perform(get("/api/activate?key={activationKey}", activationKey))
            .andExpect(status().isOk());

        Optional<User> result = userRepository.findById(user.getId());
        assertTrue(result.isPresent());
        assertThat(result.get().isActivated()).isTrue();
    }

    @Test
    @Transactional
    public void testActivateAccountWithWrongKey() throws Exception {
        restMvc.perform(get("/api/activate?key=wrongActivationKey"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional
    public void testSaveAccount() {
        executeForUserKey("test", () -> {
            UserLogin userLogin = new UserLogin();
            userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
            userLogin.setLogin("save-account");

            User user = new User();
            user.setUserKey("test");
            user.setRoleKey(ROLE_USER);
            user.setPassword(RandomStringUtils.random(60));
            user.setActivated(true);
            user.getLogins().add(userLogin);
            userLogin.setUser(user);

            userRepository.saveAndFlush(user);

            UserDTO userDTO = new UserDTO(
                user.getId(),                      // id
                "firstname",                // firstName
                "lastname",                  // lastName
                "http://placehold.it/50x50", //imageUrl
                false,                   // activated
                false,                   // tfaEnabled
                null, // tfaOtpChannelType
                null, // tfaOtpChannelSpec
                "en",                   // langKey
                null,                   // createdBy
                null,                   // createdDate
                null,                   // lastModifiedBy
                null,                   // lastModifiedDate
                RoleConstant.SUPER_ADMIN,
                "test",
                List.of("test"),
                null, null, null,
                null, Collections.singletonList(userLogin),
                Collections.emptyList(), false, null,
                null);

            try {
                restMvc.perform(
                    post("/api/account")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userDTO)))
                    .andExpect(status().isOk());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            Optional<User> updatedUserOpt = userRepository.findById(user.getId());
            assertTrue(updatedUserOpt.isPresent());
            User updatedUser = updatedUserOpt.get();
            assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
            assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
            assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
            assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
            assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
            assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
            assertThat(updatedUser.isActivated()).isEqualTo(true);
            assertThat(updatedUser.getAuthorities()).isEqualTo(userDTO.getAuthorities());
        });
    }

    @Test
    @Transactional
    public void testSaveExistingEmail() {
        executeForUserKey("test1", () -> {

            UserLogin userLogin = new UserLogin();
            userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
            userLogin.setLogin("save-existing-email");

            User user = new User();
            user.setUserKey("test");
            user.setPassword(RandomStringUtils.random(60));
            user.setActivated(true);
            user.setRoleKey(ROLE_USER);
            userLogin.setUser(user);
            user.getLogins().add(userLogin);

            userRepository.saveAndFlush(user);

            User anotherUser = new User();
            anotherUser.setUserKey("test1");
            anotherUser.setPassword(RandomStringUtils.random(60));
            anotherUser.setActivated(true);
            anotherUser.setRoleKey(ROLE_USER);

            userRepository.saveAndFlush(anotherUser);

            UserDTO userDTO = new UserDTO(
                anotherUser.getId(),         // id
                "firstname",                 // firstName
                "lastname",                  // lastName
                "http://placehold.it/50x50", //imageUrl
                false,                   // activated
                false,                   // tfaEnabled
                null, // tfaOtpChannelType
                null, // tfaOtpChannelSpec
                "en",                   // langKey
                null,                   // createdBy
                null,                   // createdDate
                null,                   // lastModifiedBy
                null,                   // lastModifiedDate
                RoleConstant.SUPER_ADMIN,
                "test1",
                List.of("test1"),
                null, null, null, null,
                Collections.singletonList(userLogin),
                Collections.emptyList(), false, null, null);

            try {
                restMvc.perform(
                    post("/api/account")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userDTO)))
                    .andExpect(status().isBadRequest());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            Optional<User> updatedUserOpt = userRepository.findById(anotherUser.getId());
            assertTrue(updatedUserOpt.isPresent());
            User updatedUser = updatedUserOpt.get();
            assertThat(updatedUser.getFirstName()).isNullOrEmpty();
            assertThat(updatedUser.getLastName()).isNullOrEmpty();
            assertThat(updatedUser.getImageUrl()).isNullOrEmpty();
        });
    }


    @Test
    @Transactional
    public void testChangePassword() {
        executeForUserKey(DEF_USER_KEY, () -> {
            String password = "password";
            User user = new User();
            user.setUserKey(DEF_USER_KEY);
            user.setRoleKey(ROLE_USER);
            user.setPassword(passwordEncoder.encode(password));

            userRepository.saveAndFlush(user);

            ChangePasswordVM vm = new ChangePasswordVM();
            vm.setOldPassword(password);
            vm.setNewPassword("1234");

            try {
                restMvc.perform(post("/api/account/change_password").contentType(
                    TestUtil.APPLICATION_JSON_UTF8).content(
                    TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isOk());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            String userKey = xmAuthenticationContextHolder.getContext().getUserKey().orElse(null);
            User updatedUser = userRepository.findOneByUserKey(userKey).orElse(null);
            assertThat(updatedUser).isNotNull();
            assertThat(passwordEncoder.matches("1234", updatedUser.getPassword())).isTrue();
        });
    }

    @Test
    @Transactional
    public void testChangePasswordTooSmall() {
        executeForUserKey(DEF_USER_KEY, () -> {
            String password = "password";
            User user = new User();
            user.setUserKey(DEF_USER_KEY);
            user.setRoleKey(ROLE_USER);
            user.setPassword(passwordEncoder.encode(password));

            userRepository.saveAndFlush(user);

            ChangePasswordVM vm = new ChangePasswordVM();
            vm.setOldPassword(password);
            vm.setNewPassword("123");

            try {
                restMvc.perform(post("/api/account/change_password").contentType(
                    TestUtil.APPLICATION_JSON_UTF8).content(
                    TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isBadRequest());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            User updatedUser = userRepository.findOneByUserKey(DEF_USER_KEY).orElse(null);
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        });
    }

    @Test
    @Transactional
    public void testChangePasswordTooLong() {
        executeForUserKey(DEF_USER_KEY, () -> {
            String password = "password";
            User user = new User();
            user.setUserKey(DEF_USER_KEY);
            user.setRoleKey(ROLE_USER);
            user.setPassword(RandomStringUtils.random(60));

            userRepository.saveAndFlush(user);

            ChangePasswordVM vm = new ChangePasswordVM();
            vm.setOldPassword(password);
            vm.setNewPassword(RandomStringUtils.random(101));

            try {
                restMvc.perform(post("/api/account/change_password").contentType(
                    TestUtil.APPLICATION_JSON_UTF8).content(
                    TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isBadRequest());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            User updatedUser = userRepository.findOneByUserKey(DEF_USER_KEY).orElse(null);
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        });
    }

    @Test
    @Transactional
    public void testChangePasswordEmpty() {
        executeForUserKey(DEF_USER_KEY, () -> {
            User user = new User();
            user.setPassword(RandomStringUtils.random(60));
            user.setUserKey(DEF_USER_KEY);
            user.setRoleKey(ROLE_USER);

            userRepository.saveAndFlush(user);

            ChangePasswordVM vm = new ChangePasswordVM();
            vm.setOldPassword(StringUtils.EMPTY);
            vm.setNewPassword(StringUtils.EMPTY);

            try {
                restMvc.perform(post("/api/account/change_password").contentType(
                    TestUtil.APPLICATION_JSON_UTF8).content(
                    TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isBadRequest());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            User updatedUser = userRepository.findOneByUserKey(DEF_USER_KEY).orElse(null);
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        });
    }

    @Test
    @Transactional
    public void testRequestPasswordReset() throws Exception {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("password-reset@example.com");

        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setRoleKey(ROLE_USER);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/reset_password/init")
            .content("password-reset@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void testRequestPasswordResetWithEmailResetType() throws Exception {
        //GIVEN
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("password-reset@example.com");

        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setRoleKey(ROLE_USER);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        userRepository.saveAndFlush(user);
        ResetPasswordVM resetPasswordVM = new ResetPasswordVM();
        resetPasswordVM.setLogin(user.getEmail());
        resetPasswordVM.setLoginType("LOGIN.EMAIL");
        resetPasswordVM.setResetType("EMAIL");

        //WHEN
        ResultActions response = restMvc.perform(post("/api/account/reset_password/init")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(resetPasswordVM)));

        //THEN
        response.andExpect(status().isOk());

        Optional<User> updatedUser = userRepository.findOneByUserKey(user.getUserKey());
        assertThat(updatedUser.isPresent()).isTrue();
        assertThat(updatedUser.get().getResetKey()).isNotNull();
    }

    @Test
    @Transactional
    public void testRequestPasswordResetWithSpecifiedResetTypeAndWrongLoginType() throws Exception {
        //GIVEN
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("password-reset@example.com");

        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setRoleKey(ROLE_USER);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        userRepository.saveAndFlush(user);
        ResetPasswordVM resetPasswordVM = new ResetPasswordVM();
        resetPasswordVM.setLogin(user.getEmail());
        resetPasswordVM.setLoginType("WRONG_LOGIN.TYPE");
        resetPasswordVM.setResetType("EMAIL");

        //WHEN
        ResultActions response = restMvc.perform(post("/api/account/reset_password/init")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(resetPasswordVM)));

        //THEN
        response.andExpect(status().isBadRequest());
    }

    @Transactional
    @Test
    public void testRequestPasswordResetWrongEmail() throws Exception {
        restMvc.perform(
            post("/api/account/reset_password/init")
                .content("password-reset-wrong-email@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void testFinishPasswordReset() throws Exception {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("password-reset@example.com");

        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key");
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("new password");

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByUserKey(DEF_USER_KEY).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    public void testFinishPasswordResetTooSmall() throws Exception {
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setRoleKey(ROLE_USER);
        user.setPassword(RandomStringUtils.random(60));
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key too small");

        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("foo");

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByResetKey(user.getResetKey()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isFalse();
    }


    @Test
    @Transactional
    public void testFinishPasswordResetWrongKey() throws Exception {
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("wrong reset key");
        keyAndPassword.setNewPassword("new password");

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testCheckResetPasswordOk() throws Exception {
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(RandomStringUtils.random(60));
        user.setResetKey("test");
        user.setResetDate(Instant.now());
        user.setRoleKey(ROLE_USER);

        user = userRepository.saveAndFlush(user);

        restMvc.perform(get("/api/account/reset_password/check?key={key}", "test"))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void testCheckResetPasswordUsed() throws Exception {
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(RandomStringUtils.random(60));
        user.setRoleKey(ROLE_USER);
        user = userRepository.saveAndFlush(user);

        restMvc.perform(get("/api/account/reset_password/check?key={key}", "test"))
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.error").value("error.reset.code.used"));
    }

    @Test
    @Transactional
    public void testCheckResetPasswordExpired() throws Exception {
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(RandomStringUtils.random(60));
        user.setResetKey("test");
        user.setResetDate(Instant.now().minusSeconds(86401));
        user.setRoleKey(ROLE_USER);

        user = userRepository.saveAndFlush(user);

        restMvc.perform(get("/api/account/reset_password/check?key={key}", "test"))
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.error").value("error.reset.code.expired"));
    }
}
