package com.icthh.xm.uaa.security;

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
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UaaAuthenticationProviderUnitTest {

    private static final String DEFAULT_USER_KEY = "TestUserKey";

    private static final String DEFAULT_USER_ROLE_KEY = "ROLE-USER";
    private static final String DEFAULT_ADMIN_ROLE_KEY = "SUPER-ADMIN";


    private AuthenticationProvider authenticationProvider = new AuthenticationProvider() {
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            return authentication;
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return true;
        }
    };

    @Mock
    private UserService userService;

    @Mock
    private TenantPropertiesService tenantPropertiesService;

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
            tenantPropertiesService);
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

    private void testUserPassword(String roleKey, Instant updatePasswordDate, int passwordExpirationPeriod) {
        tenantProperties = new TenantProperties();
        tenantProperties.setSecurity(new Security());
        tenantProperties.getSecurity().setPasswordExpirationPeriod(passwordExpirationPeriod);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        User user = new User();
        user.setRoleKey(roleKey);
        user.setUpdatePasswordDate(updatePasswordDate);
        when(userService.getUser(DEFAULT_USER_KEY)).thenReturn(user);


        Authentication authentication = mock(Authentication.class);
        DomainUserDetails userDetails = mock(DomainUserDetails.class);
        when(userDetails.getUserKey()).thenReturn(DEFAULT_USER_KEY);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        uaaAuthenticationProvider.authenticate(authentication);
    }

}
