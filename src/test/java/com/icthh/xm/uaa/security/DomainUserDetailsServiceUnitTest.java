package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
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
import static org.mockito.Mockito.when;

public class DomainUserDetailsServiceUnitTest {

    @Mock
    private UserLoginRepository userLoginRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    private DomainUserDetailsService userDetailsService;

    private User user;
    private UserLogin userLogin;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        userDetailsService = new DomainUserDetailsService(userLoginRepository, tenantContextHolder);

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

}
