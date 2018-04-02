package com.icthh.xm.uaa.service;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.mail.MailService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Transactional
public class SocialServiceIntTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmRequestContextHolder requestContextHolder;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Mock
    private MailService mockMailService;

    @Mock
    private UsersConnectionRepository mockUsersConnectionRepository;

    @Mock
    private ConnectionRepository mockConnectionRepository;

    private SocialService socialService;

    private static final String ROLE_USER = "ROLE_USER";

    @BeforeTransaction
    public void beforeTransaction() {
        tenantContextHolder.getPrivilegedContext().setTenant(buildTenant(DEFAULT_TENANT_KEY_VALUE));
    }

    @AfterTransaction
    public void afterTransaction() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Before
    @SneakyThrows
    public void setup() {
        TenantProperties.Security security = new TenantProperties.Security();
        security.setDefaultUserRole(ROLE_USER);

        TenantProperties properties = new TenantProperties();
        properties.setSecurity(security);
        properties.setRegistrationCaptchaPeriodSeconds(null);

        tenantPropertiesService.onRefresh("/config/tenants/XM/uaa/uaa.yml",
            new ObjectMapper(new YAMLFactory()).writeValueAsString(properties));

        MockitoAnnotations.initMocks(this);

        doNothing().when(mockMailService).sendSocialRegistrationValidationEmail(any(),
                                                                                anyString(),
                                                                                anyString(),
                                                                                anyString(),
                                                                                any(),
                                                                                anyString());
        doNothing().when(mockConnectionRepository).addConnection(anyObject());
        when(mockUsersConnectionRepository.createConnectionRepository(anyString())).thenReturn(mockConnectionRepository);

        socialService = new SocialService(mockUsersConnectionRepository,
                                          passwordEncoder,
                                          userRepository,
                                          mockMailService,
                                          userLoginRepository,
                                          tenantContextHolder,
                                          requestContextHolder,
                                          tenantPropertiesService);
    }

    @Test
    public void testDeleteUserSocialConnection() throws Exception {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");
        socialService.createSocialUser(connection, "fr");
        MultiValueMap<String, Connection<?>> connectionsByProviderId = new LinkedMultiValueMap<>();
        connectionsByProviderId.put("PROVIDER", null);
        when(mockConnectionRepository.findAllConnections()).thenReturn(connectionsByProviderId);

        // Exercise
        socialService.deleteUserSocialConnection("@LOGIN");

        // Verify
        verify(mockConnectionRepository, times(1)).removeConnections("PROVIDER");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSocialUserShouldThrowExceptionIfConnectionIsNull() {
        // Exercise
        socialService.createSocialUser(null, "fr");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSocialUserShouldThrowExceptionIfConnectionHasNoEmailAndNoLogin() {
        // Setup
        Connection<?> connection = createConnection("",
                                                    "",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");
    }

    @Test
    public void testCreateSocialUserShouldCreateUserIfNotExist() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        // Verify
        final Optional<UserLogin> user = userLoginRepository.findOneByLogin("mail@mail.com");
        assertThat(user).isPresent();

        // Teardown
        userRepository.delete(user.get().getUser());
    }

    @Test
    public void testCreateSocialUserShouldCreateUserWithSocialInformation() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        // Verify
        UserLogin userLogin = userLoginRepository.findOneByLogin("mail@mail.com").get();
        assertThat(userLogin.getUser().getFirstName()).isEqualTo("FIRST_NAME");
        assertThat(userLogin.getUser().getLastName()).isEqualTo("LAST_NAME");
        assertThat(userLogin.getUser().getImageUrl()).isEqualTo("IMAGE_URL");

        // Teardown
        userRepository.delete(userLogin.getUser());
    }

    @Test
    public void testCreateSocialUserShouldCreateActivatedUserWithRoleUserAndPassword() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        //Verify
        UserLogin userLogin = userLoginRepository.findOneByLogin("mail@mail.com").get();
        assertThat(userLogin.getUser().isActivated()).isEqualTo(true);
        assertThat(userLogin.getUser().getPassword()).isNotEmpty();
        assertThat(userLogin.getUser().getRoleKey()).isEqualTo(ROLE_USER);

        // Teardown
        userRepository.delete(userLogin.getUser());
    }

    @Test
    public void testCreateSocialUserShouldCreateUserWithExactLangKey() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        //Verify
        final UserLogin userLogin = userLoginRepository.findOneByLogin("mail@mail.com").get();
        assertThat(userLogin.getUser().getLangKey()).isEqualTo("fr");

        // Teardown
        userRepository.delete(userLogin.getUser());
    }

    @Test
    public void testCreateSocialUserShouldCreateUserWithLoginSameAsEmailIfNotTwitter() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER_OTHER_THAN_TWITTER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        //Verify
        UserLogin userLogin = userLoginRepository.findOneByLogin("mail@mail.com").get();
        assertThat(userLogin.getLogin()).isEqualTo("mail@mail.com");

        // Teardown
        userRepository.delete(userLogin.getUser());
    }

    @Test
    public void testCreateSocialUserShouldCreateUserWithSocialLoginWhenIsTwitter() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "twitter");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        //Verify
        UserLogin userLogin = userLoginRepository.findOneByLogin("mail@mail.com").get();
        assertThat(userLogin.getLogin()).isEqualToIgnoringCase("mail@mail.com");

        // Teardown
        userRepository.delete(userLogin.getUser());
    }

    @Test
    public void testCreateSocialUserShouldCreateSocialConnection() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        //Verify
        verify(mockConnectionRepository, times(1)).addConnection(connection);

        // Teardown
        UserLogin userToDelete = userLoginRepository.findOneByLogin("mail@mail.com").get();
        userRepository.delete(userToDelete.getUser());
    }

    @Test
    public void testCreateSocialUserShouldNotCreateUserIfEmailAlreadyExist() {
        // Setup
        createExistingUser(
            "mail@mail.com",
            "OTHER_FIRST_NAME",
            "OTHER_LAST_NAME",
            "OTHER_IMAGE_URL");
        long initialUserCount = userRepository.count();
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        //Verify
        assertThat(userRepository.count()).isEqualTo(initialUserCount);

        // Teardown
        UserLogin userToDelete = userLoginRepository.findOneByLogin("mail@mail.com").get();
        userRepository.delete(userToDelete.getUser());
    }

    @Test
    public void testCreateSocialUserShouldNotChangeUserIfEmailAlreadyExist() {
        // Setup
        createExistingUser("mail@mail.com",
                           "OTHER_FIRST_NAME",
                           "OTHER_LAST_NAME",
                           "OTHER_IMAGE_URL");
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        // Verify
        UserLogin userToVerify = userLoginRepository.findOneByLogin("mail@mail.com").get();
        assertThat(userToVerify.getLogin()).isEqualTo("mail@mail.com");
        assertThat(userToVerify.getUser().getFirstName()).isEqualTo("OTHER_FIRST_NAME");
        assertThat(userToVerify.getUser().getLastName()).isEqualTo("OTHER_LAST_NAME");
        assertThat(userToVerify.getUser().getImageUrl()).isEqualTo("OTHER_IMAGE_URL");
        // Teardown
        userRepository.delete(userToVerify.getUser());
    }

    @Test
    public void testCreateSocialUserShouldSendRegistrationValidationEmail() {
        // Setup
        Connection<?> connection = createConnection("@LOGIN",
                                                    "mail@mail.com",
                                                    "FIRST_NAME",
                                                    "LAST_NAME",
                                                    "IMAGE_URL",
                                                    "PROVIDER");

        // Exercise
        socialService.createSocialUser(connection, "fr");

        // Verify
        verify(mockMailService, times(1)).sendSocialRegistrationValidationEmail(any(),
                                                                                anyString(),
                                                                                anyString(),
                                                                                anyString(),
                                                                                any(),
                                                                                anyString());

        // Teardown
        UserLogin userToDelete = userLoginRepository.findOneByLogin("mail@mail.com").get();
        userRepository.delete(userToDelete.getUser());
    }

    private Connection<?> createConnection(String login,
                                           String email,
                                           String firstName,
                                           String lastName,
                                           String imageUrl,
                                           String providerId) {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getEmail()).thenReturn(email);
        when(userProfile.getUsername()).thenReturn(login);
        when(userProfile.getFirstName()).thenReturn(firstName);
        when(userProfile.getLastName()).thenReturn(lastName);

        Connection<?> connection = mock(Connection.class);
        ConnectionKey key = new ConnectionKey(providerId, "PROVIDER_USER_ID");
        when(connection.fetchUserProfile()).thenReturn(userProfile);
        when(connection.getKey()).thenReturn(key);
        when(connection.getImageUrl()).thenReturn(imageUrl);

        return connection;
    }

    private User createExistingUser(String email,
                                    String firstName,
                                    String lastName,
                                    String imageUrl) {
        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setImageUrl(imageUrl);
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin(email);
        userLogin.setUser(user);
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        user.getLogins().add(userLogin);
        return userRepository.saveAndFlush(user);
    }
}
