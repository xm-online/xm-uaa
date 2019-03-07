package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
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
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

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
    @IgnoreLogginAspect
    public DomainUserDetails loadUserByUsername(final String login) {
        final String lowerLogin = login.toLowerCase();
        log.debug("Authenticating {} -> {}", login, lowerLogin);
        TenantKey tenantKey = tenantContextHolder.getContext().getTenantKey()
            .orElseThrow(() -> new TenantNotProvidedException("Tenant not provided for authentication"));


        return userLoginRepository.findOneByLogin(lowerLogin).map(userLogin -> {
            User user = userLogin.getUser();
            if (!user.isActivated()) {
                throw new InvalidGrantException("User " + lowerLogin + " was not activated");
            }

            // get user login's
            List<UserLoginDto> logins = user.getLogins().stream()
                .filter(l -> !l.isRemoved())
                .map(UserLoginDto::new)
                .collect(toList());

            // get user role authority
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRoleKey());
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(authority);

            return new DomainUserDetails(lowerLogin,
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
            () -> new UsernameNotFoundException("User " + lowerLogin
                                                    + " was not found for tenant " + tenantKey.getValue()));
    }

}
