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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
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

        if (userOpt.isPresent()) {
            updateUser(ctx, userOpt.get(), authorities);
        } else {
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
        userDTO.setRoleKey(mapRole(ldapConf.getRole(), authorities));

        userService.createUser(userDTO);
    }

    private void updateUser(DirContextOperations ctx, User user, Collection<? extends GrantedAuthority> authorities) {
        String mappedRole = mapRole(ldapConf.getRole(), authorities);
        log.info("Mapped role from ldap [{}], current role [{}]", mappedRole, user.getRoleKey());

        if (!mappedRole.equals(user.getRoleKey())) {
            user.setResetKey(mappedRole);
            userService.saveUser(user);
        }
    }

    private String mapRole(TenantProperties.Ldap.Role roleConf, Collection<? extends GrantedAuthority> authorities) {
        String mappedXmRole = roleConf.getDefaultRole();
        LinkedList<String> mappedRoles = new LinkedList<>();
        Map<String, String> mappingConf = roleConf.getMapping();

        if (mappingConf != null) {
            mappingConf.forEach((ldapRole, xmRole) -> {
                boolean matched = authorities.stream().anyMatch(a -> ldapRole.equals(a.getAuthority()));
                if (matched) {
                    mappedRoles.add(xmRole);
                }
            });
        }
        if (mappedRoles.isEmpty()) {
            log.info("Role mapping not found. Default role {} will be used", mappedXmRole);
        } else {
            mappedXmRole = mappedRoles.getLast();
        }

        if (mappedRoles.size() > BigInteger.ONE.intValue()) {
            log.warn("More than 1 role was matched: {}. Will be used the latest one: {}", mappedRoles, mappedXmRole);
        }

        log.info("Mapped role: {}", mappedXmRole);
        return mappedXmRole;
    }
}
