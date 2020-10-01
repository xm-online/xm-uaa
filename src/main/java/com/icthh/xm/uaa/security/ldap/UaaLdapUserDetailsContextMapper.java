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
import org.apache.commons.lang3.StringUtils;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

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
        userDTO.setImageUrl(parseImageUrl(ctx));

        //login mapping
        TenantProperties.Ldap.Role roleConf = ldapConf.getRole();
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin(username);
        userLogin.setTypeKey(UserLoginType.NICKNAME.getValue());
        userDTO.setLogins(Collections.singletonList(userLogin));
        userDTO.setRoleKey(defaultIfEmpty(mapRole(roleConf, authorities), roleConf.getDefaultRole()));
        userService.createUser(userDTO);
    }

    private void updateUser(DirContextOperations ctx, User user, Collection<? extends GrantedAuthority> authorities) {
        TenantProperties.Ldap.Role roleConf = ldapConf.getRole();
        String mappedRole = defaultIfEmpty(mapRole(roleConf, authorities), user.getRoleKey());
        mappedRole = defaultIfEmpty(mappedRole, roleConf.getDefaultRole());
        log.info("Mapped role from ldap [{}], current role [{}]", mappedRole, user.getRoleKey());
        String imageUrl = parseImageUrl(ctx);

        if (needUpdate(mappedRole, user, imageUrl)) {
            user.setImageUrl(imageUrl);
            user.setRoleKey(mappedRole);
            userService.saveUser(user);
        }
    }

    private boolean needUpdate(String mappedRole, User user, String imageUrl) {
        return !mappedRole.equals(user.getRoleKey()) || !StringUtils.equals(user.getImageUrl(), imageUrl);
    }

    private String mapRole(TenantProperties.Ldap.Role roleConf, Collection<? extends GrantedAuthority> authorities) {
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
        String mappedXmRole = mappedRoles.isEmpty() ? null : mappedRoles.getLast();
        if (mappedRoles.size() > BigInteger.ONE.intValue()) {
            log.warn("More than 1 role was matched: {}. Will be used the latest one: {}", mappedRoles, mappedXmRole);
        }

        log.info("Mapped role: {}", mappedXmRole);
        return mappedXmRole;
    }

    private String parseImageUrl(DirContextOperations ctx) {
        String imageUrl = ldapConf.getImageUrl();
        String dynamicParameterPattern = ldapConf.getDynamicParameterPattern();
        if (nonNull(imageUrl) && nonNull(dynamicParameterPattern)) {
            ImageUrlParser imageUrlParser = ImageUrlParser.parser(imageUrl, dynamicParameterPattern);
            List<String> parameters = imageUrlParser.getParameters();
            Map<String, String> paramsMap = parameters.stream()
                .collect(Collectors.toMap(Function.identity(), param -> getAttribute(ctx, param)));
            return imageUrlParser.replace(paramsMap);
        }
        return null;

    }

    private String getAttribute(DirContextOperations ctx, String param) {
        try {
            return String.valueOf(ctx.getAttributes().get(param).get());
        } catch (NamingException e) {
            log.error("Cannot find {} in Active Directory", param, e);
        }
        return null;

    }
}
