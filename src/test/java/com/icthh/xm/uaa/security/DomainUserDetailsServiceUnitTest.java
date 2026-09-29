package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;

import java.util.Optional;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DomainUserDetailsServiceUnitTest {

    @Mock
    private UserLoginRepository userLoginRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    private DomainUserDetailsService userDetailsService;
    private ApplicationProperties applicationProperties;

    private User user;
    private UserLogin userLogin;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        applicationProperties = new ApplicationProperties();
        userDetailsService = new DomainUserDetailsService(
            userLoginRepository, tenantContextHolder, applicationProperties);

        userLogin = new UserLogin();
        userLogin.setLogin("admin");
        user = new User();
        user.setActivated(true);
        user.setUserKey("test");
        user.setPassword("password");
        user.setRoleKey(RoleConstant.SUPER_ADMIN);
        userLogin.setUser(user);
    }

    @Test
    public void testLoginSuccess() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("XM")));
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");

        assertEquals("admin", result.getUsername());
        assertEquals("XM", result.getTenant());
        assertEquals("test", result.getUserKey());
    }

    @Test
    public void testLoginWithMixedCaseUsernameFindsLowercaseLoginByExactMatch() {
        String lowerLogin = "tst-mw-xwiki@vodafone.ua";
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("XM")));
        userLogin.setLogin(lowerLogin);
        when(userLoginRepository.findOneByLogin(eq(lowerLogin)))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername("tst-MW-xwiki@vodafone.ua");

        assertEquals(lowerLogin, result.getUsername());
        verify(userLoginRepository).findOneByLogin(eq(lowerLogin));
        verify(userLoginRepository, never()).findOneByLoginIgnoreCase(eq(lowerLogin));
    }

    @Test
    public void testLoginWithMixedCaseStoredLoginUsesCaseInsensitiveFallback() {
        String login = "tst-MW-xwiki@vodafone.ua";
        String lowerLogin = "tst-mw-xwiki@vodafone.ua";
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("XM")));
        applicationProperties.getSecurity().setCaseInsensitiveLoginFallbackEnabled(true);
        userLogin.setLogin(login);
        when(userLoginRepository.findOneByLogin(eq(lowerLogin)))
            .thenReturn(Optional.empty());
        when(userLoginRepository.findOneByLoginIgnoreCase(eq(lowerLogin)))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername(login);

        assertEquals(lowerLogin, result.getUsername());
        verify(userLoginRepository).findOneByLogin(eq(lowerLogin));
        verify(userLoginRepository).findOneByLoginIgnoreCase(eq(lowerLogin));
    }

    @Test
    public void testLoginWithUnknownUserDoesNotUseFallbackWhenDisabled() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(DEFAULT_TENANT_KEY_VALUE)));
        when(userLoginRepository.findOneByLogin(eq("unknown")))
            .thenReturn(Optional.empty());

        try {
            userDetailsService.loadUserByUsername("unknown");
        } catch (UsernameNotFoundException expected) {
            verify(userLoginRepository, never()).findOneByLoginIgnoreCase(eq("unknown"));
            return;
        }

        throw new AssertionError("Expected UsernameNotFoundException");
    }

    @Test(expected = TenantNotProvidedException.class)
    public void testLoginNoTenant() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.empty());
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        userDetailsService.loadUserByUsername("admin");
    }

    @Test(expected = InvalidGrantException.class)
    public void testLoginUserNotActivated() {
        user.setActivated(false);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(DEFAULT_TENANT_KEY_VALUE)));
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");
    }

    @Test(expected = UsernameNotFoundException.class)
    public void testLoginUserNotFound() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(DEFAULT_TENANT_KEY_VALUE)));
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.empty());

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");
    }

    @Test
    public void testLoginWithLeadingAndTrailingSpaces() {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(DEFAULT_TENANT_KEY_VALUE)));
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername(" admin    ");

        assertEquals("admin", result.getUsername());
    }
}
