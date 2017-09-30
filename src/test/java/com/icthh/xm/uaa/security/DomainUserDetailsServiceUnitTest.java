package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.config.tenant.TenantInfo;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class DomainUserDetailsServiceUnitTest {

    private static final String DEFAULT_TENANT = "XM";

    @Mock
    private UserLoginRepository userLoginRepository;

    @Mock
    private TenantInfo tenantInfo;

    private DomainUserDetailsService userDetailsService;

    private User user;
    private UserLogin userLogin;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantContext.setCurrent(tenantInfo);
        userDetailsService = new DomainUserDetailsService(userLoginRepository);

        userLogin = new UserLogin();
        userLogin.setLogin("admin");
        user = new User();
        user.setActivated(true);
        user.setUserKey("test");
        user.setPassword("password");
        userLogin.setUser(user);
    }

    @Test
    public void testLoginSuccess() {
        when(tenantInfo.getTenant()).thenReturn(DEFAULT_TENANT);
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");

        assertEquals("admin", result.getUsername());
        assertEquals("XM", result.getTenant());
        assertEquals("test", result.getUserKey());
    }

    @Test(expected = TenantNotProvidedException.class)
    public void testLoginNoHeader() {
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");
    }

    @Test(expected = TenantNotProvidedException.class)
    public void testLoginEmptyHeader() {
        when(tenantInfo.getTenant()).thenReturn(" ");
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");
    }

    @Test(expected = UserNotActivatedException.class)
    public void testLoginUserNotActivated() {
        user.setActivated(false);
        when(tenantInfo.getTenant()).thenReturn(DEFAULT_TENANT);
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.of(userLogin));

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");
    }

    @Test(expected = UsernameNotFoundException.class)
    public void testLoginUserNotFound() {
        when(tenantInfo.getTenant()).thenReturn(DEFAULT_TENANT);
        when(userLoginRepository.findOneByLogin(eq("admin")))
            .thenReturn(Optional.empty());

        DomainUserDetails result = userDetailsService.loadUserByUsername("admin");
    }
}
