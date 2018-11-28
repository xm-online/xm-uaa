package com.icthh.xm.uaa.security.ldap;

import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class UaaLdapUserDetailsContextMapper extends LdapUserDetailsMapper {

    private final DomainUserDetailsService userDetailsService;
    private final UserService userService;
    private final TenantProperties.Ldap ldapConf;

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx,
                                          String username,
                                          Collection<? extends GrantedAuthority> authorities) {

        Optional<User> userOpt = userService.findOneByLogin(username);
        if (!userOpt.isPresent()) {
            createUser(ctx, username, authorities);
        }

        return userDetailsService.loadUserByUsername(username);
    }

    private void createUser(DirContextOperations ctx,
                            String username,
                            Collection<? extends GrantedAuthority> authorities) {
        UserDTO userDTO = new UserDTO();
        //base info mapping
        userDTO.setFirstName(ctx.getStringAttribute(ldapConf.getAttribute().getFirstName()));
        userDTO.setLastName(ctx.getStringAttribute(ldapConf.getAttribute().getLastName()));

        //login mapping
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin(username);
        userLogin.setTypeKey(UserLoginType.NICKNAME.getValue());
        userDTO.setLogins(Collections.singletonList(userLogin));

        //role mapping
        if (authorities.isEmpty()) {
            userDTO.setRoleKey(ldapConf.getRole().getDefaultRole());
        }
        userDTO.setRoleKey(authorities.iterator().next().getAuthority());

        userService.createUser(userDTO);
    }
}
