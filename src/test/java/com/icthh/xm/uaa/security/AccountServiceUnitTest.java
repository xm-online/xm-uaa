package com.icthh.xm.uaa.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.GrantType;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.oauth2.otp.EmailOtpSender;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSenderFactory;
import com.icthh.xm.uaa.security.oauth2.otp.SmsOtpSender;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.messaging.UserMessagingService;
import com.icthh.xm.uaa.web.rest.vm.AuthorizeUserVm;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

        doNothing().when(emailOtpSender).send(any());
        doNothing().when(smsOtpSender).send(any());

        when(otpSenderFactory.getSender(OtpChannelType.EMAIL)).thenReturn(Optional.of(emailOtpSender));
        when(otpSenderFactory.getSender(OtpChannelType.SMS)).thenReturn(Optional.of(smsOtpSender));

        accountService = new AccountService(userRepository, passwordEncoder, registrationLogRepository,
            authContextHolder, tenantPropertiesService, userService, userLoginService, profileEventProducer,
            otpSenderFactory);
    }

    @Test
    public void test_authorizeAccount_shouldRegisterNewUserAndReturnOtpGrandType() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey("LOGIN.EMAIL");
        userLogin.setLogin(USERNAME_EMAIL);

        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(USERNAME_EMAIL);
        authorizeUserVm.setLangKey("ua");

        when(userService.findOneByLogin(USERNAME_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(mockUser);

        String result = accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR);

        assertNotNull(result);
        assertEquals(GrantType.OTP.getValue(), result);

        verify(userService).findOneByLogin(eq(USERNAME_EMAIL));
        verify(userLoginService).normalizeLogins(eq(List.of(userLogin)));
        verify(userLoginService).normalizeLogins(eq(List.of(userLogin)));
        verify(passwordEncoder).encode(any());
        verify(registrationLogRepository).save(any());
        verify(userMessagingService, never()).sendAuthorizeMessage(any(OtpSendDTO.class));
        verify(profileEventProducer).createEventJson(any(), any());
        verify(profileEventProducer).send(any());
        verify(emailOtpSender).send(any());
        verify(smsOtpSender, never()).send(any());
        verify(userRepository, times(2)).save(any());
    }

    @Test
    public void test_authorizeAccount_shouldAuthorizeRegisteredUserAndReturnOtpGrandType() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey("LOGIN.EMAIL");
        userLogin.setLogin(USERNAME_SMS);

        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(USERNAME_SMS);
        authorizeUserVm.setLangKey("ua");

        when(userService.findOneByLogin(USERNAME_SMS)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        String result = accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR);

        assertNotNull(result);
        assertEquals(GrantType.OTP.getValue(), result);

        verify(userService).findOneByLogin(eq(USERNAME_SMS));
        verify(userLoginService, never()).normalizeLogins(eq(List.of(userLogin)));
        verify(userLoginService, never()).normalizeLogins(eq(List.of(userLogin)));
        verify(passwordEncoder, never()).encode(any());
        verify(registrationLogRepository, never()).save(any());
        verify(userMessagingService, never()).sendAuthorizeMessage(any(OtpSendDTO.class));
        verify(profileEventProducer, never()).createEventJson(any(), any());
        verify(profileEventProducer, never()).send(any());
        verify(emailOtpSender, never()).send(any());
        verify(smsOtpSender).send(any());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    public void test_authorizeAccount_shouldAuthorizeRegisteredUserAndReturnPasswordGrandType() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey("LOGIN.EMAIL");
        userLogin.setLogin(USERNAME_SMS);

        AuthorizeUserVm authorizeUserVm = new AuthorizeUserVm();
        authorizeUserVm.setLogin(USERNAME_SMS);
        authorizeUserVm.setLangKey("ua");

        when(mockUser.getPasswordSetByUser()).thenReturn(true);
        when(userService.findOneByLogin(USERNAME_SMS)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        String result = accountService.authorizeAccount(authorizeUserVm, REMOTE_ADDR);

        assertNotNull(result);
        assertEquals(GrantType.PASSWORD.getValue(), result);

        verify(userService).findOneByLogin(eq(USERNAME_SMS));
        verify(userLoginService, never()).normalizeLogins(eq(List.of(userLogin)));
        verify(userLoginService, never()).normalizeLogins(eq(List.of(userLogin)));
        verify(passwordEncoder, never()).encode(any());
        verify(registrationLogRepository, never()).save(any());
        verify(userMessagingService, never()).sendAuthorizeMessage(any(OtpSendDTO.class));
        verify(profileEventProducer, never()).createEventJson(any(), any());
        verify(profileEventProducer, never()).send(any());
        verify(emailOtpSender, never()).send(any());
        verify(smsOtpSender, never()).send(any());
        verify(userRepository, never()).save(any());
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

        assertThrows("can not build UserLoginType from value: " + login, IllegalArgumentException.class, () ->
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
}
