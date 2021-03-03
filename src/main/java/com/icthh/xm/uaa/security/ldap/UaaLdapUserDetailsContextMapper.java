package com.icthh.xm.uaa.security.ldap;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.naming.NamingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

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
        List<String> roles = mapRole(roleConf, authorities);
        userDTO.setAuthorities(roles.isEmpty() ? List.of(roleConf.getDefaultRole()) : roles);
        userService.createUser(userDTO);
    }

    private void updateUser(DirContextOperations ctx, User user, Collection<? extends GrantedAuthority> authorities) {
        TenantProperties.Ldap.Role roleConf = ldapConf.getRole();
        List<String> mappedRoles = mapRole(roleConf, authorities);
        mappedRoles = mappedRoles.isEmpty() ?
            (isEmpty(user.getAuthorities()) ? List.of(roleConf.getDefaultRole()) : user.getAuthorities()) :
            mappedRoles;
        log.info("Mapped role from ldap [{}], current role [{}]", mappedRoles, user.getAuthorities());
        String imageUrl = parseImageUrl(ctx);

        if (needUpdate(mappedRoles, user, imageUrl)) {
            user.setImageUrl(imageUrl);
            user.setAuthorities(mappedRoles);
            userService.saveUser(user);
        }
    }

    private boolean needUpdate(List<String> mappedRoles, User user, String imageUrl) {
        return !isEqualCollection(mappedRoles, user.getAuthorities()) || !StringUtils.equals(user.getImageUrl(), imageUrl);
    }

    private List<String> mapRole(TenantProperties.Ldap.Role roleConf, Collection<? extends GrantedAuthority> authorities) {
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
        log.info("Mapped role: {}", mappedRoles);
        return mappedRoles;
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
