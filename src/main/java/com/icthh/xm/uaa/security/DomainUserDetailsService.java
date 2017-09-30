package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Service("userDetailsService")
@AllArgsConstructor
@Slf4j
public class DomainUserDetailsService implements UserDetailsService {

    private final UserLoginRepository userLoginRepository;

    @Override
    @Transactional
    public DomainUserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);
        String tenantName = TenantContext.getCurrent().getTenant();
        if (StringUtils.isNotBlank(tenantName)) {
            return userLoginRepository.findOneByLogin(login).map(userLogin -> {
                if (!userLogin.getUser().isActivated()) {
                    throw new UserNotActivatedException("User " + login + " was not activated");
                }
                List<GrantedAuthority> grantedAuthorities = userLogin.getUser().getAuthorities().stream()
                    .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                    .collect(Collectors.toList());
                return new DomainUserDetails(login,
                    userLogin.getUser().getPassword(),
                    grantedAuthorities,
                    tenantName,
                    userLogin.getUser().getUserKey(),
                    userLogin.getUser().getAccessTokenValiditySeconds(),
                    userLogin.getUser().getRefreshTokenValiditySeconds());
            }).orElseThrow(
                () -> new UsernameNotFoundException("User " + login
                    + " was not found for tenant " + tenantName));
        } else {
            throw new TenantNotProvidedException("Tenant not provided for authentication");
        }
    }
}
