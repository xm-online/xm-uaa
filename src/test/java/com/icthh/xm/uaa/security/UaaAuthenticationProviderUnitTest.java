package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Security;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UaaAuthenticationProviderUnitTest {

    private static final String DEFAULT_USER_KEY = "TestUserKey";

    private static final String DEFAULT_USER_ROLE_KEY = "ROLE-USER";
    private static final String DEFAULT_ADMIN_ROLE_KEY = "SUPER-ADMIN";
    @Mock
    private AuthenticationProvider authenticationProvider;

    @Mock
    private UserService userService;

    @Mock
    private TenantPropertiesService tenantPropertiesService;

    @Mock
    private ApplicationProperties applicationProperties;

    private UaaAuthenticationProvider uaaAuthenticationProvider;

    private TenantProperties tenantProperties;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        tenantProperties = new TenantProperties();
        tenantProperties.setSecurity(new Security());
        tenantProperties.getSecurity().setPasswordExpirationPeriod(5);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        uaaAuthenticationProvider = new UaaAuthenticationProvider(authenticationProvider,
                                                                  null,
                                                                  userService,
                                                                  tenantPropertiesService,
                                                                  applicationProperties);
        uaaAuthenticationProvider.setSelf(uaaAuthenticationProvider);
    }

    @Test
    public void testCheckUserPasswordExpirationSuccess(){
        testUserPassword(DEFAULT_USER_ROLE_KEY, Instant.now().minusSeconds(3600), 5);
    }

    @Test (expected = CredentialsExpiredException.class)
    public void testCheckUserPasswordExpirationFailed(){
        testUserPassword(DEFAULT_USER_ROLE_KEY, Instant.now().minus(7, ChronoUnit.DAYS), 5);
    }

    @Test
    public void testCheckSuperAdminPasswordExpirationSuccess(){
        testUserPassword(DEFAULT_ADMIN_ROLE_KEY, Instant.now().minusSeconds(3600), 5);
    }

    @Test
    public void testCheckSuperAdminPasswordExpirationFailed(){
        testUserPassword(DEFAULT_ADMIN_ROLE_KEY, Instant.now().minus(7, ChronoUnit.DAYS), 5);
    }

    @Test(expected = BadCredentialsException.class)
    public void testUserAuthenticationFailed(){
        Authentication authentication = mock(Authentication.class);
        DomainUserDetails userDetails = mock(DomainUserDetails.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        doThrow(BadCredentialsException.class).when(authenticationProvider).authenticate(authentication);
        doNothing().when(userService).increaseFailedPasswordAttempts(DEFAULT_USER_KEY);

        uaaAuthenticationProvider.authenticate(authentication);

        verify(authenticationProvider).authenticate(authentication);
        verify(userService).increaseFailedPasswordAttempts(DEFAULT_USER_KEY);
        verifyNoMoreInteractions(authenticationProvider, userService);
    }

    private void testUserPassword(String roleKey, Instant updatePasswordDate, int passwordExpirationPeriod) {
        tenantProperties = new TenantProperties();
        tenantProperties.setSecurity(new Security());
        tenantProperties.getSecurity().setPasswordExpirationPeriod(passwordExpirationPeriod);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        User user = new User();
        user.setRoleKey(roleKey);
        user.setUpdatePasswordDate(updatePasswordDate);
        when(userService.getUser(DEFAULT_USER_KEY)).thenReturn(user);
        doNothing().when(userService).onSuccessfulLogin(DEFAULT_USER_KEY);


        Authentication authentication = mock(Authentication.class);
        DomainUserDetails userDetails = mock(DomainUserDetails.class);
        when(userDetails.getUserKey()).thenReturn(DEFAULT_USER_KEY);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationProvider.authenticate(authentication)).thenReturn(authentication);
        uaaAuthenticationProvider.authenticate(authentication);
    }
}
