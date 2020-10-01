package com.icthh.xm.uaa.security.ldap;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

@RunWith(MockitoJUnitRunner.class)
public class UaaLdapUserDetailsContextMapperTest {

    private static final String LOGIN = "Homer";
    public static final Map<String, String> GROUP_MAPPING = Map.of(
        "GROUP1", "SUPER_ADMIN",
        "GROUP2", "LOSER"
    );
    @Mock
    UserService userService;

    @Mock
    DirContextOperations ctx;

    @Mock
    TenantProperties.Ldap ldapConf;

    @Mock
    DomainUserDetailsService userDetailsService;

    @InjectMocks
    UaaLdapUserDetailsContextMapper uaaLdapUserDetailsContextMapper;

    @Test
    public void userRoleMustNotChangeDuringLogin() {
        User user = whenFindUserByLogin();
        when(ldapConf.getRole()).thenReturn(new TenantProperties.Ldap.Role("DEFAULT_ROLE", null));
        uaaLdapUserDetailsContextMapper.mapUserFromContext(ctx, LOGIN, List.of());
        verify(userDetailsService).loadUserByUsername(eq(LOGIN));
        verify(userService, never()).saveUser(any(User.class));
        assertEquals(user.getRoleKey(), "ROLE_ADMIN");
    }

    @Test
    public void useRoleMustChangeToOneFromADGroup() {
        whenFindUserByLogin();
        when(ldapConf.getRole()).thenReturn(new TenantProperties.Ldap.Role("DEFAULT_ROLE", GROUP_MAPPING));
        uaaLdapUserDetailsContextMapper.mapUserFromContext(ctx, LOGIN, newArrayList((GrantedAuthority) () -> "GROUP1"));
        verify(userDetailsService).loadUserByUsername(eq(LOGIN));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService, times(1))
            .saveUser(captor.capture());
        User user = captor.getValue();
        assertEquals(user.getRoleKey(), "SUPER_ADMIN");
    }


    @Test
    public void userMustBeCreatedWithDefaultRole() {
        when(ldapConf.getRole()).thenReturn(new TenantProperties.Ldap.Role("DEFAULT_ROLE", null));
        when(ldapConf.getAttribute()).thenReturn(new TenantProperties.Ldap.Attribute("Homer", "Simpson"));
        uaaLdapUserDetailsContextMapper.mapUserFromContext(ctx, LOGIN, newArrayList((GrantedAuthority) () -> "GROUP1"));
        verify(userDetailsService).loadUserByUsername(eq(LOGIN));

        ArgumentCaptor<UserDTO> captor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userService, times(1))
            .createUser(captor.capture());
        UserDTO user = captor.getValue();
        assertEquals(user.getRoleKey(), "DEFAULT_ROLE");
    }

    private User whenFindUserByLogin() {
        User user = new User();
        user.setRoleKey("ROLE_ADMIN");
        when(userService.findOneByLogin(LOGIN)).thenAnswer(invocation -> of(user));
        return user;
    }
}
