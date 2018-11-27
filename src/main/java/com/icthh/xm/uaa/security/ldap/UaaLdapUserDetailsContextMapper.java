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
            UserDTO userDTO = new UserDTO();
            userDTO.setFirstName(ctx.getStringAttribute(ldapConf.getAttribute().getFirstName()));
            userDTO.setLastName(ctx.getStringAttribute(ldapConf.getAttribute().getLastName()));

            UserLogin userLogin = new UserLogin();
            userLogin.setLogin(username);
            userLogin.setTypeKey(UserLoginType.NICKNAME.getValue());

            userDTO.setLogins(Collections.singletonList(userLogin));
            userDTO.setRoleKey(authorities.stream().findFirst().get().getAuthority());

            userService.createUser(userDTO);
        }

        //todo create user if not exist
        return userDetailsService.loadUserByUsername(username);
    }
}
