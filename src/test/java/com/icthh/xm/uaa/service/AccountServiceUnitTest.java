package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.GrantType;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.oauth2.otp.EmailOtpSender;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSenderFactory;
import com.icthh.xm.uaa.security.oauth2.otp.SmsOtpSender;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.messaging.UserMessagingService;
import com.icthh.xm.uaa.service.util.RandomUtil;
import com.icthh.xm.uaa.web.rest.vm.AuthorizeUserVm;
import liquibase.util.StringUtils;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.icthh.xm.uaa.config.Constants.LOGIN_INVALID_CODE;
import static com.icthh.xm.uaa.config.Constants.OTP_THROTTLING_ERROR_TEXT;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AccountServiceUnitTest {

    private static final TenantKey TENANT_KEY = TenantKey.valueOf("XM");

    private static final String OTP_CODE = "123456";
    private static final String USERNAME_EMAIL = "user@test.com.ua";
    private static final String USERNAME_SMS = "050749502758";
    private static final String REMOTE_ADDR = "remoteAddr";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String USER_KEY = UUID.randomUUID().toString();

    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private UserMessagingService userMessagingService;
    @Mock
    private XmRequestContextHolder xmRequestContextHolder;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RegistrationLogRepository registrationLogRepository;
    @Mock
    private XmAuthenticationContextHolder authContextHolder;
    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private UserService userService;
    @Mock
    private UserLoginService userLoginService;
    @Mock
    private ProfileEventProducer profileEventProducer;
    @Mock
    private OtpSenderFactory otpSenderFactory;

    @Mock
    private SmsOtpSender smsOtpSender;

    @Mock
    private EmailOtpSender emailOtpSender;

    private AccountService accountService;

    private User mockUser;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TENANT_KEY));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        XmRequestContext xmRequestContext = mock(XmRequestContext.class);
        when(xmRequestContextHolder.getContext()).thenReturn(xmRequestContext);

        TenantProperties tenantProperties = buildTenantProperties();
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        mockUser = mock(User.class);
        when(mockUser.getLangKey()).thenReturn("ua");
        when(mockUser.getTfaOtpSecret()).thenReturn(Base32.random());

        when(otpSenderFactory.getSender(OtpChannelType.EMAIL)).thenReturn(Optional.of(emailOtpSender));
        when(otpSenderFactory.getSender(OtpChannelType.SMS)).thenReturn(Optional.of(smsOtpSender));

        accountService = new AccountService(userRepository, passwordEncoder, registrationLogRepository,
            authContextHolder, tenantPropertiesService, userService, userLoginService, profileEventProducer,
            otpSenderFactory);

        accountService.setSelf(accountService);
    }

    @Test
    public void test_authorizeAccount_shouldRegisterNewUserAndReturnOtpGrandType() {
        User regiteredUser = buildUser(USERNAME_EMAIL, false);
        UserLogin userLogin = regiteredUser.getLogins().iterator().next();

        assertNull(regiteredUser.getOtpCode());
        assertNull(regiteredUser.getOtpCodeCreationDate());

        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(userLogin.getLogin());
        authorizeUserVm.setLangKey("ua");

        when(userService.findOneByLogin(userLogin.getLogin())).thenReturn(Optional.empty());
        when(userRepository.save(argThat(new RegisteredUserMatcher(regiteredUser)))).thenReturn(regiteredUser);

        String result = accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR);

        // prepare expected data
        TenantProperties.NotificationChannel expectedChannel = buildTenantProperties().getCommunication()
            .getNotifications().get("auth-otp")
            .getChannels().get("email");
        OtpSendDTO expectedOtpSendDTO = new OtpSendDTO(OTP_CODE, userLogin.getLogin(), regiteredUser.getUserKey(), expectedChannel);

        // check result
        assertNotNull(result);
        assertEquals(GrantType.OTP.getValue(), result);

        assertNotNull(regiteredUser.getOtpCode());
        assertNotNull(regiteredUser.getOtpCodeCreationDate());

        verify(userService).findOneByLogin(eq(userLogin.getLogin()));
        verify(userLoginService).normalizeLogins(eq(List.of(userLogin)));
        verify(passwordEncoder).encode(anyString());
        verify(registrationLogRepository).save(argThat(a -> REMOTE_ADDR.equals(a.getIpAddress())));
        verify(profileEventProducer)
            .createEventJson(argThat(new DtoFromUserMatcher(regiteredUser)), eq(Constants.CREATE_PROFILE_EVENT_TYPE));
        verify(profileEventProducer).send(any());
        verify(emailOtpSender).send(argThat(new OtpSendDTOMatcher(expectedOtpSendDTO)));
        verify(userRepository).save(regiteredUser);
        verify(userRepository)
            .save(argThat(a -> StringUtils.isNotEmpty(a.getOtpCode()) && a.getOtpCodeCreationDate() != null));

        verifyNoMoreInteractions(userMessagingService);
        verifyNoMoreInteractions(smsOtpSender);
    }

    @Test
    public void test_authorizeAccount_shouldAuthorizeRegisteredUserAndReturnOtpGrandType() {
        User regiteredUser = buildUser(USERNAME_SMS, false);
        String login = regiteredUser.getLogins().iterator().next().getLogin();

        assertNull(regiteredUser.getOtpCode());
        assertNull(regiteredUser.getOtpCodeCreationDate());

        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(login);
        authorizeUserVm.setLangKey("ua");

        when(userService.findOneByLogin(login)).thenReturn(Optional.of(regiteredUser));
        when(userRepository.save(regiteredUser)).thenReturn(regiteredUser);

        String result = accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR);

        // prepare expected data
        TenantProperties.NotificationChannel expectedChannel = buildTenantProperties().getCommunication()
            .getNotifications().get("auth-otp")
            .getChannels().get("sms");
        OtpSendDTO expectedOtpSendDTO = new OtpSendDTO(OTP_CODE, login, regiteredUser.getUserKey(), expectedChannel);

        // check result
        assertNotNull(result);
        assertEquals(GrantType.OTP.getValue(), result);

        assertNotNull(regiteredUser.getOtpCode());
        assertNotNull(regiteredUser.getOtpCodeCreationDate());

        verify(userService).findOneByLogin(eq(login));
        verify(smsOtpSender).send(argThat(new OtpSendDTOMatcher(expectedOtpSendDTO)));
        verify(userRepository, times(1)).save(regiteredUser);

        verifyNoMoreInteractions(userLoginService);
        verifyNoMoreInteractions(passwordEncoder);
        verifyNoMoreInteractions(registrationLogRepository);
        verifyNoMoreInteractions(userMessagingService);
        verifyNoMoreInteractions(profileEventProducer);
        verifyNoMoreInteractions(emailOtpSender);
    }

    @Test
    public void test_authorizeAccount_shouldAuthorizeRegisteredUserAndReturnPasswordGrandType() {
        User regiteredUser = buildUser(USERNAME_SMS, true);
        String login = regiteredUser.getLogins().iterator().next().getLogin();

        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(login);
        authorizeUserVm.setLangKey("ua");

        when(userService.findOneByLogin(login)).thenReturn(Optional.of(regiteredUser));

        String result = accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR);

        assertNotNull(result);
        assertEquals(GrantType.PASSWORD.getValue(), result);

        verify(userService).findOneByLogin(eq(login));

        verifyNoMoreInteractions(userLoginService);
        verifyNoMoreInteractions(passwordEncoder);
        verifyNoMoreInteractions(registrationLogRepository);
        verifyNoMoreInteractions(userMessagingService);
        verifyNoMoreInteractions(profileEventProducer);
        verifyNoMoreInteractions(emailOtpSender);
        verifyNoMoreInteractions(smsOtpSender);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    public void test_authorizeAccount_requestOtpOften_shouldThrowBusinessException() {
        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(USERNAME_SMS);
        authorizeUserVm.setLangKey("ua");

        when(mockUser.getOtpCodeCreationDate()).thenReturn(Instant.now());
        when(userService.findOneByLogin(USERNAME_SMS)).thenReturn(Optional.of(mockUser));

        assertThrows(OTP_THROTTLING_ERROR_TEXT, BusinessException.class, () ->
            accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR)
        );
    }

    @Test
    public void test_authorizeAccount_invalidLogin_shouldThrowBusinessException() {
        String login = RandomStringUtils.random(10);

        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(login);
        authorizeUserVm.setLangKey("ua");

        when(userService.findOneByLogin(USERNAME_SMS)).thenReturn(Optional.of(mockUser));

        assertThrows("User login type could not be determined by value: " + login, BusinessException.class, () ->
            accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR)
        );
    }

    @Test
    public void test_sendOtpCode_requestOtpOften_shouldThrowBusinessException() {
        when(mockUser.getOtpCodeCreationDate()).thenReturn(Instant.now());
        when(userService.findOneByLogin(USERNAME_EMAIL)).thenReturn(Optional.of(mockUser));

        assertThrows(OTP_THROTTLING_ERROR_TEXT, BusinessException.class, () ->
            accountService.sendOtpCode(USERNAME_EMAIL)
        );
    }

    @Test
    public void test_sendOtpCode_sendOtpCode_shouldThrowBusinessException() {
        String login = RandomStringUtils.random(10);

        when(userService.findOneByLogin(login)).thenReturn(Optional.of(mockUser));

        assertThrows(LOGIN_INVALID_CODE, BusinessException.class, () ->
            accountService.sendOtpCode(login)
        );
    }

    @Test
    public void test_sendOtpCode_userNotFound_shouldThrowBusinessException() {
        when(userService.findOneByLogin(USERNAME_EMAIL)).thenReturn(Optional.empty());

        assertThrows(String.format("User by login '%s' not found", USERNAME_EMAIL), BusinessException.class, () ->
            accountService.sendOtpCode(USERNAME_EMAIL)
        );
    }

    public User buildUser(String username, boolean passSetByUser) {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.getByRegex(username).getValue());
        userLogin.setLogin(username);

        User user = new User();
        user.setId(1L);
        user.setUserKey(UUID.randomUUID().toString());
        user.setPassword(org.apache.commons.lang3.RandomStringUtils.random(60));
        user.setPasswordSetByUser(passSetByUser);
        user.setLangKey("ua");
        user.setActivationKey(RandomUtil.generateActivationKey());
        user.setRoleKey("ROLE_VIEW");
        user.setLogins(List.of(userLogin));
        user.getLogins().forEach(ul -> ul.setUser(user));
        return user;
    }

    private TenantProperties buildTenantProperties() {
        TenantProperties.NotificationChannel channel1 = new TenantProperties.NotificationChannel();
        channel1.setKey("SMS_NOTIFICATION");
        channel1.setType("Twilio");
        channel1.setTemplateName("sms.template.name");

        TenantProperties.NotificationChannel channel2 = new TenantProperties.NotificationChannel();
        channel2.setKey("EMAIL_NOTIFICATION");
        channel2.setType("TemplatedEmail");
        channel2.setTemplateName("email.template.name");

        TenantProperties.Notification notification = new TenantProperties.Notification();
        notification.setChannels(Map.of("sms", channel1, "email", channel2));

        TenantProperties.Communication communication = new TenantProperties.Communication();
        communication.setEnabled(true);
        communication.setNotifications(Map.of("auth-otp", notification));

        TenantProperties.Security security = new TenantProperties.Security();
        security.setDefaultUserRole(ROLE_USER);

        TenantProperties properties = new TenantProperties();
        properties.setCommunication(communication);
        properties.setSecurity(security);
        return properties;
    }

    class OtpSendDTOMatcher implements ArgumentMatcher<OtpSendDTO> {

        private final OtpSendDTO expected;

        OtpSendDTOMatcher(OtpSendDTO expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(OtpSendDTO actual) {
            assertEquals(expected.getDestination(), actual.getDestination());
            assertEquals(expected.getUserKey(), actual.getUserKey());
            assertEquals(expected.getTemplateName(), actual.getTemplateName());
            assertEquals(expected.getTitleKey(), actual.getTitleKey());
            assertEquals(expected.getChannel().getKey(), actual.getChannel().getKey());
            assertEquals(expected.getChannel().getType(), actual.getChannel().getType());
            assertEquals(expected.getChannel().getTemplateName(), actual.getChannel().getTemplateName());
            assertEquals(expected.getChannel().getTitleKey(), actual.getChannel().getTitleKey());
            return true;
        }

    }

    class RegisteredUserMatcher implements ArgumentMatcher<User> {

        private final User expected;

        RegisteredUserMatcher(User expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(User actual) {
            assertEquals(expected.getPasswordSetByUser(), actual.getPasswordSetByUser());
            assertEquals(expected.isActivated(), actual.isActivated());
            assertEquals(expected.getLangKey(), actual.getLangKey());
            assertEquals(expected.getLogins().size(), actual.getLogins().size());
            assertEquals(expected.getLogins().iterator().next().getLogin(), actual.getLogins().iterator().next().getLogin());
            assertEquals(expected.getLogins().iterator().next().getTypeKey(), actual.getLogins().iterator().next().getTypeKey());
            return true;
        }
    }

    class DtoFromUserMatcher implements ArgumentMatcher<UserDTO> {

        private final User expected;

        DtoFromUserMatcher(User expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(UserDTO actual) {
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getLangKey(), actual.getLangKey());
            assertEquals(expected.getLogins().size(), actual.getLogins().size());
            assertEquals(expected.getLogins().iterator().next().getLogin(), actual.getLogins().iterator().next().getLogin());
            assertEquals(expected.getLogins().iterator().next().getTypeKey(), actual.getLogins().iterator().next().getTypeKey());
            return true;
        }
    }
}
