package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.security.TokenConstraintsService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UserServiceUnitTest {

    private static final String USER_KEY = "userKey";
    private static final Long ID = 1L;
    private static final String USER_LOGIN = "userLogin";

    @InjectMocks
    private UserService service;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLoginRepository userLoginRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;
    @Mock
    private TokenConstraintsService tokenConstraintsService;
    @Mock
    private ApplicationProperties applicationProperties;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service.setSelf(service);
    }

    @Test
    public void testRequestPasswordResetWithResetByEmail() {
        //GIVEN
        User user = createUser();
        user.setActivated(true);

        UserLogin userLogin = createUserLogin(user);
        userLogin.setTypeKey("LOGIN.EMAIL");

        given(userLoginRepository.findOneByLoginIgnoreCase(USER_LOGIN)).willReturn(Optional.of(userLogin));

        //WHEN
        Optional<User> actualLogin = service.requestPasswordResetForLoginWithType(USER_LOGIN, UserLoginType.EMAIL);

        //THEN
        assertThat(actualLogin.isPresent()).isTrue();
        assertThat(actualLogin.get().getResetKey()).isNotNull();
    }

    @Test
    public void testCompletePasswordResetError() {
        given(userRepository.findOneByResetKey(USER_KEY)).willReturn(Optional.empty());
        given(tenantPropertiesService.getTenantProps()).willReturn(new TenantProperties());
        assertThatThrownBy(() -> {
            service.completePasswordReset("123",USER_KEY);
        }).hasMessage("Reset code used");
    }

    @Test
    public void testCompletePasswordReset() {
        User u = createUser();
        u.setResetDate(Instant.now());
        given(userRepository.findOneByResetKey(USER_KEY)).willReturn(Optional.of(u));
        given(tenantPropertiesService.getTenantProps()).willReturn(new TenantProperties());
        given(passwordEncoder.encode("123")).willReturn("321");
        User nu = service.completePasswordReset("123", USER_KEY);
        assertThat(u.getUserKey()).isEqualTo(nu.getUserKey());
        assertThat(nu.getResetDate()).isNull();
        assertThat(nu.getResetKey()).isNull();
    }

    @Test
    public void testFailPasswordResetDateExpired() {
        User u = createUser();
        u.setResetDate(Instant.now().minus(100000L, DAYS));
        given(userRepository.findOneByResetKey(USER_KEY)).willReturn(Optional.of(u));
        given(tenantPropertiesService.getTenantProps()).willReturn(new TenantProperties());
        assertThatThrownBy(() -> {
            service.completePasswordReset("123", USER_KEY);
        }).hasMessage("Reset code expired");
    }

    @Test
    public void shouldForbidToBlockSelfAccount() {
        given(xmAuthenticationContextHolder.getContext()).willReturn(getDummyCTX());
        assertThatThrownBy(() -> {
            service.blockUserAccount(USER_KEY);
        }).hasMessage("Forbidden to block himself");

    }

    @Test
    public void shouldBlockAccount() {
        User d = createUser(USER_KEY+"1", "X");
        given(xmAuthenticationContextHolder.getContext()).willReturn(getDummyCTX());
        given(userRepository.findOneByUserKey(d.getUserKey())).willReturn(Optional.of(d));
        Optional<UserDTO> result = service.blockUserAccount(d.getUserKey());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().isActivated()).isFalse();
        assertThat(result.get().getUserKey()).isEqualTo(d.getUserKey());
        assertThat(result.get().getId()).isEqualTo(d.getId());
    }

    @Test
    public void shouldForbidToActivateSelfAccount() {
        given(xmAuthenticationContextHolder.getContext()).willReturn(getDummyCTX());
        assertThatThrownBy(() -> {
            service.activateUserAccount(USER_KEY);
        }).hasMessage("Forbidden to activate himself");

    }

    @Test
    public void shouldDeleteUser() {
        String userKey = "uk";
        given(xmAuthenticationContextHolder.getContext()).willReturn(getDummyCTX());
        given(userRepository.findOneWithLoginsByUserKey(userKey)).willReturn(Optional.of(createUser(userKey, "dummyRole")));
        service.deleteUser(userKey);
        verify(userRepository, times(1)).delete(any());
    }

    @Test
    public void shouldDeleteUserAndCallNotification() {
        String userKey = "uk";
        given(xmAuthenticationContextHolder.getContext()).willReturn(getDummyCTX());
        given(userRepository.findOneWithLoginsByUserKey(userKey)).willReturn(Optional.of(createUser(userKey, "dummyRole")));
        Consumer<UserDTO> callBack = mockLambda(Consumer.class, userDto -> System.out.println("userKey=" + userDto));
        service.deleteUser(userKey, callBack);
        verify(userRepository, times(1)).delete(any());
        verify(callBack, times(1)).accept(any());
    }

    @Test
    public void shouldNotDeleteUserAndCallNotification() {
        String userKey = "uk";
        given(xmAuthenticationContextHolder.getContext()).willReturn(getDummyCTX());
        given(userRepository.findOneWithLoginsByUserKey(userKey)).willReturn(Optional.of(createUser(userKey, "dummyRole")));
        Consumer<UserDTO> callBack = mockLambda(Consumer.class, userDto -> System.out.println("userKey=" + userDto));
        service.deleteUser("UNDEFINED_KEY", callBack);
        verify(userRepository, times(0)).delete(any());
        verify(callBack, times(0)).accept(any());
    }

    @Test
    public void shouldChangeAccountState() {
        User d = createUser(USER_KEY+"1", "X");
        d.setActivated(Boolean.FALSE);
        given(xmAuthenticationContextHolder.getContext()).willReturn(getDummyCTX());
        given(userRepository.findOneByUserKey(d.getUserKey())).willReturn(Optional.of(d));
        Optional<UserDTO> result = service.activateUserAccount(d.getUserKey());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().isActivated()).isTrue();
        assertThat(result.get().getUserKey()).isEqualTo(d.getUserKey());
        assertThat(result.get().getId()).isEqualTo(d.getId());
    }

    @Test
    public void shouldFailIfRoleCodeIsEmptyOrNull() {
        UserDTO userDTO = new UserDTO();
        userDTO.setRoleKey(null);
        assertThatThrownBy(() -> {
            service.changeUserRole(userDTO);
        }).hasMessage("No roleKey provided");
        userDTO.setRoleKey("");
        assertThatThrownBy(() -> {
            service.changeUserRole(userDTO);
        }).hasMessage("No roleKey provided");
    }

    @Test
    public void shouldFailForSuperAdmin() {
        UserDTO userDTO = new UserDTO();
        userDTO.setRoleKey("X");
        userDTO.setId(ID);
        given(userRepository.findById(ID)).willReturn(Optional.of(createUser(USER_KEY, SUPER_ADMIN)));
        assertThatThrownBy(() -> {
            service.changeUserRole(userDTO);
        }).hasMessage("This operation can not be applied to SUPER-ADMIN");
    }

    @Test
    public void shouldChangeUserRole() {
        UserDTO userDTO = new UserDTO();
        userDTO.setRoleKey("X");
        userDTO.setId(ID);
        given(userRepository.findById(ID)).willReturn(Optional.of(createUser(USER_KEY, "Y")));
        Optional<UserDTO> result = service.changeUserRole(userDTO);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getRoleKey()).isEqualTo(userDTO.getRoleKey());
    }

    @Test
    public void shouldUpdateRoleAndStateIfStrictUserManagementFalse() {

        UserDTO newUser = new UserDTO();
        newUser.setId(ID);
        newUser.setUserKey("USERX");
        newUser.setRoleKey("ROLEX");
        newUser.setFirstName("fn");
        newUser.setLastName("ln");
        newUser.setCreatedBy("cb");
        newUser.setImageUrl("newUrl");
        newUser.setLangKey("XXXX");
        newUser.setAccessTokenValiditySeconds(100);
        newUser.setAutoLogoutTimeoutSeconds(200);
        newUser.setActivated(Boolean.FALSE);

        User oldUser = createUser("USERX", "Y");
        oldUser.setFirstName("fn1");
        oldUser.setLastName("ln1");
        oldUser.setCreatedBy("cb!!!");
        oldUser.setImageUrl("oldUrl");
        oldUser.setActivationKey("oldKey");
        oldUser.setAccessTokenValiditySeconds(1000);
        oldUser.setAutoLogoutTimeoutSeconds(2000);
        oldUser.setResetDate(null);
        oldUser.setActivated(Boolean.TRUE);

        given(userRepository.findById(ID)).willReturn(Optional.of(oldUser));
        given(tokenConstraintsService.getAccessTokenValiditySeconds(oldUser.getAccessTokenValiditySeconds())).willReturn(10);
        given(tenantPropertiesService.getTenantProps()).willReturn(new TenantProperties());

        UserDTO result = service.updateUser(newUser).get();
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(newUser.getFirstName());
        assertThat(result.getLastName()).isEqualTo(newUser.getLastName());
        assertThat(result.getImageUrl()).isEqualTo(newUser.getImageUrl());
        assertThat(result.getLangKey()).isEqualTo(newUser.getLangKey());

        assertThat(result.getCreatedBy()).isEqualTo(oldUser.getCreatedBy());
        assertThat(result.getRoleKey()).isEqualTo("ROLEX");
        assertThat(result.isActivated()).isEqualTo(Boolean.FALSE);

    }

    @Test
    public void shouldNotUpdateRoleAndStateIfStrictUserManagementTrue() {

        UserDTO newUser = new UserDTO();
        newUser.setId(ID);
        newUser.setUserKey("USERX");
        newUser.setRoleKey("ROLEX");
        newUser.setFirstName("fn");
        newUser.setLastName("ln");
        newUser.setCreatedBy("cb");
        newUser.setImageUrl("newUrl");
        newUser.setLangKey("XXXX");
        newUser.setAccessTokenValiditySeconds(100);
        newUser.setAutoLogoutTimeoutSeconds(200);
        newUser.setActivated(Boolean.FALSE);

        User oldUser = createUser("USERX", "Y");
        oldUser.setFirstName("fn1");
        oldUser.setLastName("ln1");
        oldUser.setCreatedBy("cb!!!");
        oldUser.setImageUrl("oldUrl");
        oldUser.setActivationKey("oldKey");
        oldUser.setAccessTokenValiditySeconds(1000);
        oldUser.setAutoLogoutTimeoutSeconds(2000);
        oldUser.setResetDate(null);
        oldUser.setActivated(Boolean.TRUE);

        given(userRepository.findById(ID)).willReturn(Optional.of(oldUser));
        given(tokenConstraintsService.getAccessTokenValiditySeconds(oldUser.getAccessTokenValiditySeconds())).willReturn(10);
        TenantProperties tp = new TenantProperties();
        tp.setStrictUserManagement(true);
        given(tenantPropertiesService.getTenantProps()).willReturn(tp);

        UserDTO result = service.updateUser(newUser).get();
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(newUser.getFirstName());
        assertThat(result.getLastName()).isEqualTo(newUser.getLastName());
        assertThat(result.getImageUrl()).isEqualTo(newUser.getImageUrl());
        assertThat(result.getLangKey()).isEqualTo(newUser.getLangKey());

        assertThat(result.getCreatedBy()).isEqualTo(oldUser.getCreatedBy());
        assertThat(result.getRoleKey()).isEqualTo("Y");
        assertThat(result.isActivated()).isEqualTo(Boolean.TRUE);

    }

    @Test
    public void getUser() {
        User user = createUser();
        when(userRepository.findOneByUserKey(USER_KEY)).thenReturn(Optional.ofNullable(user));
        assertEquals(user, service.getUser(USER_KEY));

        verify(userRepository).findOneByUserKey(USER_KEY);
    }

    @Test
    public void saveUser() {
        User user = createUser();
        when(userRepository.save(user)).thenReturn(user);
        service.saveUser(user);

        verify(userRepository).save(user);
    }

    @Test
    public void shouldIncreasePasswordAttemptsAndDisableUser() {
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.getSecurity().setMaxPasswordAttempts(3);

        User user = createUser();
        user.setPasswordAttempts(2);

        UserLogin userLogin = createUserLogin(user);

        given(tenantPropertiesService.getTenantProps()).willReturn(tenantProperties);
        given(userLoginRepository.findOneByLoginIgnoreCase(USER_LOGIN)).willReturn(Optional.of(userLogin));

        service.increaseFailedPasswordAttempts(USER_LOGIN);

        assertThat(user.getPasswordAttempts()).isEqualTo(0);
        assertThat(user.isActivated()).isFalse();

        verify(tenantPropertiesService).getTenantProps();
        verify(userLoginRepository).findOneByLoginIgnoreCase(USER_LOGIN);
    }

    @Test
    public void shouldNotIncreasePasswordAttemptsIfPropertyDoesNotExists() {
        given(tenantPropertiesService.getTenantProps()).willReturn(new TenantProperties());

        service.increaseFailedPasswordAttempts(USER_LOGIN);

        verifyZeroInteractions(userLoginRepository);
    }

    @Test
    public void shouldResetPasswordAttemptsIfLoginSuccess(){
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.getSecurity().setMaxPasswordAttempts(3);
        User user = createUser();
        user.setPasswordAttempts(2);

        given(tenantPropertiesService.getTenantProps()).willReturn(tenantProperties);
        given(userRepository.findOneByUserKey(USER_KEY)).willReturn(Optional.of(user));
        given(applicationProperties.isLastLoginDateEnabled()).willReturn(Boolean.FALSE);

        service.onSuccessfulLogin(USER_KEY);

        assertThat(user.getPasswordAttempts()).isEqualTo(0);

        verify(tenantPropertiesService).getTenantProps();
        verify(userRepository).findOneByUserKey(USER_KEY);
        verify(applicationProperties).isLastLoginDateEnabled();
    }

    private User createUser() {
        User user = new User();
        user.setId(ID);
        user.setUserKey(USER_KEY);
        return user;
    }

    private User createUser(String userKey, String roleKey) {
        User user = createUser();
        user.setUserKey(userKey);
        user.setRoleKey(roleKey);
        user.setActivated(Boolean.TRUE);
        return user;
    }

    private UserLogin createUserLogin(User user) {
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin(USER_LOGIN);
        userLogin.setUser(user);
        return userLogin;
    }

    private XmAuthenticationContext getDummyCTX() {
        return new XmAuthenticationContext() {
            @Override
            public boolean hasAuthentication() {
                return false;
            }

            @Override
            public boolean isAnonymous() {
                return false;
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public boolean isRememberMe() {
                return false;
            }

            @Override
            public boolean isFullyAuthenticated() {
                return false;
            }

            @Override
            public Optional<String> getLogin() {
                return Optional.empty();
            }

            @Override
            public String getRequiredLogin() {
                return null;
            }

            @Override
            public Optional<String> getRemoteAddress() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getSessionId() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getUserKey() {
                return Optional.of(USER_KEY);
            }

            @Override
            public String getRequiredUserKey() {
                return USER_KEY;
            }

            @Override
            public Optional<String> getTokenValue() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getTokenType() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getDetailsValue(String key) {
                return Optional.empty();
            }

            @Override
            public String getDetailsValue(String key, String defaultValue) {
                return null;
            }

            @Override
            public Optional<String> getAdditionalDetailsValue(String key) {
                return Optional.empty();
            }

            @Override
            public String getAdditionalDetailsValue(String key, String defaultValue) {
                return null;
            }
        };
    }

    public static <T> T mockLambda(final Class<T> lambdaType, final T lambda) {
        return mock(lambdaType, delegatesTo(lambda));
    }

}
