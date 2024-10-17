package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * Authenticate a user from the database.
 */
@LepService(group = "service.user.details")
@Service("userDetailsService")
@AllArgsConstructor
@Slf4j
public class DomainUserDetailsService implements UserDetailsService {

    private final UserLoginRepository userLoginRepository;
    private final TenantContextHolder tenantContextHolder;

    @Override
    @Transactional
    @IgnoreLogginAspect
    @LogicExtensionPoint("LoadUserByUsername")
    public DomainUserDetails loadUserByUsername(final String login) {
        final String lowerLogin = login.toLowerCase().trim();

        String tenantKey = getTenantKey();

        log.debug("Authenticating login: {}, lowercase: {}, within tenant: {}", login, lowerLogin, tenantKey);

        return retrieveUserByUsername(login).orElseThrow(buildException(lowerLogin, tenantKey));
    }

    public Optional<DomainUserDetails> retrieveUserByUsername(final String login) {
        final String lowerLogin = login.toLowerCase().trim();

        String tenantKey = getTenantKey();

        log.debug("Retrieving user with login: {}, lowercase: {}, within tenant: {}", login, lowerLogin, tenantKey);

        return userLoginRepository
            .findOneByLogin(lowerLogin)
            .map(userLogin -> buildDomainUserDetails(lowerLogin, tenantKey, userLogin.getUser()));
    }

    private String getTenantKey() {
        return tenantContextHolder.getContext()
            .getTenantKey()
            .map(TenantKey::getValue)
            .orElseThrow(() -> new TenantNotProvidedException("Tenant not provided for authentication"));
    }

    private Supplier<UsernameNotFoundException> buildException(String lowerLogin, String tenantKey) {
        return () -> {
            log.error("User [{}] was not found for tenant [{}]", lowerLogin, tenantKey);
            return new UsernameNotFoundException("User " + lowerLogin + " was not found for tenant " + tenantKey);
        };
    }

    public static DomainUserDetails buildDomainUserDetails(String lowerLogin, String tenantKey, User user) {
        if (!user.isActivated()) {
            throw new InvalidGrantException("User " + lowerLogin + " was not activated");
        }

        // get user login's
        List<UserLoginDto> logins = user.getLogins().stream()
                                        .filter(l -> !l.isRemoved())
                                        .map(UserLoginDto::new)
                                        .collect(toList());

        // get user role authority
        List<SimpleGrantedAuthority> authorities = user.getAuthorities()
            .stream()
            .map(SimpleGrantedAuthority::new)
            .collect(toList());

        return new DomainUserDetails(
            lowerLogin,
            user.getPassword(),
            authorities,
            tenantKey,
            user.getUserKey(),
            user.getOtpCode(),
            user.isTfaEnabled(),
            user.getTfaOtpSecret(),
            user.getTfaOtpChannelType(),
            user.getAccessTokenValiditySeconds(),
            user.getRefreshTokenValiditySeconds(),
            user.getTfaAccessTokenValiditySeconds(),
            user.isAutoLogoutEnabled(),
            user.getAutoLogoutTimeoutSeconds(),
            logins,
            user.getLangKey()
        );
    }

}
