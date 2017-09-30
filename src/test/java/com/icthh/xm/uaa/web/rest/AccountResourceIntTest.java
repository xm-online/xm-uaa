package com.icthh.xm.uaa.web.rest;

import static com.icthh.xm.uaa.config.Constants.DEFAULT_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.errors.ExceptionTranslator;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.config.tenant.TenantInfo;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.Authority;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.AuthorityRepository;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.AuthoritiesConstants;
import com.icthh.xm.uaa.service.*;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.rest.vm.ChangePasswordVM;
import com.icthh.xm.uaa.web.rest.vm.KeyAndPasswordVM;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the AccountResource REST controller.
 *
 * @see AccountResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UaaApp.class, XmOverrideConfiguration.class})
public class AccountResourceIntTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private HttpMessageConverter[] httpMessageConverters;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Mock
    private ProfileEventProducer profileEventProducer;

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    private MockMvc restUserMockMvc;

    private MockMvc restMvc;

    private static final String DEF_USER_KEY = "def_user_key";

    @BeforeClass
    public static void init() {
        TenantContext.setDefault();
    }

    @AfterClass
    public static void tearDown() {
        TenantContext.clear();
    }

    @Before
    @SneakyThrows
    public void setup() {
        TenantProperties properties = new TenantProperties();
        properties.setRegistrationCaptchaPeriodSeconds(null);
        tenantPropertiesService.onRefresh("/config/tenants/"+ DEFAULT_TENANT + "/uaa/uaa.yml",
            new ObjectMapper(new YAMLFactory()).writeValueAsString(properties));

        TenantContext.setCurrent(DEFAULT_TENANT);

        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail(anyObject(), any(), any(), any());
        doNothing().when(profileEventProducer).send(any());

        RegistrationLogRepository registrationLogRepository = mock(RegistrationLogRepository.class);
        when(registrationLogRepository.findOneByIpAddress(any())).thenReturn(Optional.empty());

        CaptchaService captchaService = new CaptchaService(applicationProperties, registrationLogRepository,
            tenantPropertiesService);

        AccountResource accountResource = new AccountResource(userRepository, userLoginRepository,
                        userService, accountService, mockMailService, profileEventProducer, captchaService);

        AccountResource accountUserMockResource = new AccountResource(userRepository,
                        userLoginRepository, mockUserService, accountService, mockMailService,
                        profileEventProducer, captchaService);

        this.restMvc = MockMvcBuilders.standaloneSetup(accountResource)
            .setMessageConverters(httpMessageConverters).setControllerAdvice(exceptionTranslator)
            .build();
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource).build();
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
    public void testGetExistingAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);

        User user = new User();
        user.setUserKey("test");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setAuthorities(authorities);
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin("email");
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        user.getLogins().add(userLogin);
        when(mockUserService.findOneWithAuthoritiesAndLoginsByUserKey(anyString())).thenReturn(Optional.of(user));

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
            .andExpect(jsonPath("$.langKey").value("en"))
            .andExpect(jsonPath("$.logins[0].login").value("email"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));

    }

    @Test
    public void testGetUnknownAccount() throws Exception {
        when(mockUserService.findOneWithAuthoritiesAndLoginsByUserKey(anyString())).thenReturn(Optional.empty());

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
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
            "http://placehold.it/50x50",  //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            new HashSet<>(Collections.singletonList(AuthoritiesConstants.USER)), "test",
            null, null, null, Collections.singletonList(login));

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
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            new HashSet<>(Collections.singletonList(AuthoritiesConstants.USER)), "test",
            null, null, null, Collections.singletonList(login));

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
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            new HashSet<>(Collections.singletonList(AuthoritiesConstants.USER)), "test",
            null, null, null, Collections.singletonList(login));

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
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            new HashSet<>(Collections.singletonList(AuthoritiesConstants.USER)), "test",
            null, null, null, Collections.singletonList(login));

        // Duplicate login, different login
        UserLogin loginNew = new UserLogin();
        loginNew.setLogin("alicejr@example.com");
        loginNew.setTypeKey(UserLoginType.NICKNAME.getValue());

        ManagedUserVM duplicatedUser = new ManagedUserVM(validUser.getId(), validUser.getPassword(),
            validUser.getFirstName(), validUser.getLastName(),
            true, validUser.getImageUrl(), validUser.getLangKey(), validUser.getCreatedBy(),
            validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate(),
            validUser.getAuthorities(), "test", validUser.getAccessTokenValiditySeconds(), validUser.getRefreshTokenValiditySeconds(), null, Arrays.asList(login, loginNew));

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
            "http://placehold.it/50x50", //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            new HashSet<>(Collections.singletonList(AuthoritiesConstants.ADMIN)), "test",
            null, null, null, Collections.singletonList(login));

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<UserLogin> userDup = userLoginRepository.findOneByLoginIgnoreCase("joe@example.com");
        assertThat(userDup.isPresent()).isTrue();
        assertThat(userDup.get().getUser().getAuthorities()).hasSize(1)
            .containsExactly(authorityRepository.findOne(AuthoritiesConstants.USER));
    }

    @Test
    @Transactional
    public void testActivateAccount() throws Exception {
        final String activationKey = "some activation key";
        User user = new User();
        user.setUserKey("test");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(false);
        user.setActivationKey(activationKey);

        user = userRepository.saveAndFlush(user);

        restMvc.perform(get("/api/activate?key={activationKey}", activationKey))
            .andExpect(status().isOk());

        user = userRepository.findOne(user.getId());
        assertThat(user.isActivated()).isTrue();
    }

    @Test
    @Transactional
    public void testActivateAccountWithWrongKey() throws Exception {
        restMvc.perform(get("/api/activate?key=wrongActivationKey"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional
    public void testSaveAccount() throws Exception {
        TenantContext.setCurrent(new TenantInfo("XM", null, "test", null, null, null, null));
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("save-account");

        User user = new User();
        user.setUserKey("test");
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
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            new HashSet<>(Collections.singletonList(AuthoritiesConstants.ADMIN)), "test",
            null, null, null, Collections.singletonList(userLogin));

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOne(user.getId());
        assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
        assertThat(updatedUser.isActivated()).isEqualTo(true);
        assertThat(updatedUser.getAuthorities()).isEmpty();
    }

    @Test
    @Transactional
    public void testSaveExistingEmail() throws Exception {
        TenantContext.setCurrent(new TenantInfo("XM", null, "test1", null, null, null, null));

        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("save-existing-email");

        User user = new User();
        user.setUserKey("test");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        userLogin.setUser(user);
        user.getLogins().add(userLogin);

        userRepository.saveAndFlush(user);

        User anotherUser = new User();
        anotherUser.setUserKey("test1");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);

        userRepository.saveAndFlush(anotherUser);

        UserDTO userDTO = new UserDTO(
            anotherUser.getId(),                   // id
            "firstname",               // firstName
            "lastname",                  // lastName
            "http://placehold.it/50x50", //imageUrl
            false,                   // activated
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            new HashSet<>(Collections.singletonList(AuthoritiesConstants.ADMIN)),
            "test1",
            null, null, null, Collections.singletonList(userLogin)
        );

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOne(anotherUser.getId());
        assertThat(updatedUser.getFirstName()).isNullOrEmpty();
        assertThat(updatedUser.getLastName()).isNullOrEmpty();
        assertThat(updatedUser.getImageUrl()).isNullOrEmpty();
    }



    @Test
    @Transactional
    public void testChangePassword() throws Exception {
        TenantContext.setCurrent(new TenantInfo("XM", null, DEF_USER_KEY, null, null, null, null));
        String password = "password";
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.saveAndFlush(user);

        ChangePasswordVM vm = new ChangePasswordVM();
        vm.setOldPassword(password);
        vm.setNewPassword("1234");

        restMvc.perform(post("/api/account/change_password").contentType(
                        TestUtil.APPLICATION_JSON_UTF8).content(
                        TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isOk());

        User updatedUser = userRepository.findOneByUserKey(TenantContext.getCurrent().getUserKey()).orElse(null);
        assertThat(passwordEncoder.matches("1234", updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    public void testChangePasswordTooSmall() throws Exception {
        TenantContext.setCurrent(new TenantInfo("XM", null, DEF_USER_KEY, null, null, null, null));
        String password = "password";
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.saveAndFlush(user);

        ChangePasswordVM vm = new ChangePasswordVM();
        vm.setOldPassword(password);
        vm.setNewPassword("123");

        restMvc.perform(post("/api/account/change_password").contentType(
                        TestUtil.APPLICATION_JSON_UTF8).content(
                        TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByUserKey(DEF_USER_KEY).orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    public void testChangePasswordTooLong() throws Exception {
        TenantContext.setCurrent(new TenantInfo("XM", null, DEF_USER_KEY, null, null, null, null));
        String password = "password";
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
        user.setPassword(RandomStringUtils.random(60));

        userRepository.saveAndFlush(user);

        ChangePasswordVM vm = new ChangePasswordVM();
        vm.setOldPassword(password);
        vm.setNewPassword(RandomStringUtils.random(101));

        restMvc.perform(post("/api/account/change_password").contentType(
            TestUtil.APPLICATION_JSON_UTF8).content(
            TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByUserKey(DEF_USER_KEY).orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    public void testChangePasswordEmpty() throws Exception {
        TenantContext.setCurrent(new TenantInfo("XM", null, DEF_USER_KEY, null, null, null, null));
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setUserKey(DEF_USER_KEY);

        userRepository.saveAndFlush(user);

        ChangePasswordVM vm = new ChangePasswordVM();
        vm.setOldPassword(StringUtils.EMPTY);
        vm.setNewPassword(StringUtils.EMPTY);

        restMvc.perform(post("/api/account/change_password").contentType(
            TestUtil.APPLICATION_JSON_UTF8).content(
            TestUtil.convertObjectToJsonBytes(vm))).andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByUserKey(DEF_USER_KEY).orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
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
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/reset_password/init")
            .content("password-reset@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    public void testRequestPasswordResetWrongEmail() throws Exception {
        restMvc.perform(
            post("/api/account/reset_password/init")
                .content("password-reset-wrong-email@example.com"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testFinishPasswordReset() throws Exception {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("password-reset@example.com");

        User user = new User();
        user.setUserKey(DEF_USER_KEY);
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
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    public void testFinishPasswordResetTooSmall() throws Exception {
        User user = new User();
        user.setUserKey(DEF_USER_KEY);
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
}
