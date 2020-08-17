package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.service.dto.UserLoginDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
        if (StringUtils.isBlank(login)) {
            throw new IllegalArgumentException("Email must be not blank");
        }
        String tenantKey = getTenant();
        return findUserByUsername(login, tenantKey).orElseThrow(buildException(login, tenantKey));
    }

    @Transactional
    @IgnoreLogginAspect
    public Optional<DomainUserDetails> findUserByUsername(final String login) {
        return findUserByUsername(login, getTenant());
    }

    private Optional<DomainUserDetails> findUserByUsername(final String login, final String tenantKey) {
        final String lowerLogin = login.toLowerCase();

        log.debug("Authenticating login: {}, lowercase: {}, within tenant: {}", login, lowerLogin, tenantKey);

        return userLoginRepository.findOneByLogin(lowerLogin)
            .map(userLogin -> buildDomainUserDetails(lowerLogin, tenantKey, userLogin));
    }

    private String getTenant() {
        return tenantContextHolder.getContext()
            .getTenantKey()
            .map(TenantKey::getValue)
            .orElseThrow(() -> new TenantNotProvidedException("Tenant not provided for authentication"));
    }

    private Supplier<UsernameNotFoundException> buildException(String lowerLogin, String tenantKey){
        return () -> {
            log.error("User [{}] was not found for tenant [{}]", lowerLogin, tenantKey);
            return new UsernameNotFoundException("User " + lowerLogin + " was not found for tenant " + tenantKey);
        };
    }

    private DomainUserDetails buildDomainUserDetails(String lowerLogin, String tenantKey, UserLogin userLogin) {
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
                                     tenantKey,
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
    }

}
