package com.icthh.xm.uaa.security;

import static java.util.stream.Collectors.toList;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.service.dto.UserLoginDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Authenticate a user from the database.
 */
@Service("userDetailsService")
@AllArgsConstructor
@Slf4j
public class DomainUserDetailsService implements UserDetailsService {

    private final UserLoginRepository userLoginRepository;
    private final TenantContextHolder tenantContextHolder;

    @Override
    @Transactional
    public DomainUserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);
        TenantKey tenantKey = tenantContextHolder.getContext().getTenantKey()
            .orElseThrow(() -> new TenantNotProvidedException("Tenant not provided for authentication"));

        return userLoginRepository.findOneByLogin(login).map(userLogin -> {
            User user = userLogin.getUser();
            if (!user.isActivated()) {
                throw new UserNotActivatedException("User " + login + " was not activated");
            }

            // get user login's
            List<UserLoginDto> logins = user.getLogins().stream()
                .filter(l -> !l.isRemoved())
                .map(UserLoginDto::new)
                .collect(toList());

            // get user role authority
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRoleKey());
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(authority);

            return new DomainUserDetails(login,
                                         user.getPassword(),
                                         authorities,
                                         tenantKey.getValue(),
                                         user.getUserKey(),
                                         user.isTfaEnabled(),
                                         user.getTfaOtpSecret(),
                                         user.getTfaOtpChannelType(),
                                         user.getAccessTokenValiditySeconds(),
                                         user.getRefreshTokenValiditySeconds(),
                                         user.getTfaAccessTokenValiditySeconds(),
                                         user.isAutoLogoutEnabled(),
                                         user.getAutoLogoutTimeoutSeconds(),
                                         logins);
        }).orElseThrow(
            () -> new UsernameNotFoundException("User " + login
                                                    + " was not found for tenant " + tenantKey.getValue()));
    }

}
