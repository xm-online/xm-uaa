package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.security.ldap.LdapAuthenticationProviderBuilder;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;

import static com.icthh.xm.uaa.config.Constants.AUTH_USERNAME_DOMAIN_SEPARATOR;
import static java.time.ZoneOffset.UTC;

@Slf4j
@IgnoreLogginAspect
@LepService(group = "security.provider")
public class UaaAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider defaultProvider;
    private final LdapAuthenticationProviderBuilder providerBuilder;
    private final UserService userService;
    private final TenantPropertiesService tenantPropertiesService;

    public UaaAuthenticationProvider(@Qualifier("daoAuthenticationProvider")
                                     @Lazy AuthenticationProvider defaultProvider,
                                     LdapAuthenticationProviderBuilder providerBuilder,
                                     UserService userService,
                                     TenantPropertiesService tenantPropertiesService) {
        this.defaultProvider = defaultProvider;
        this.providerBuilder = providerBuilder;
        this.userService = userService;
        this.tenantPropertiesService = tenantPropertiesService;
    }

    private AuthenticationProvider getProvider(Authentication authentication) {
        AuthenticationProvider provider = defaultProvider;
        String principal = authentication.getPrincipal().toString();
        LinkedList<String> parts = new LinkedList<>(Arrays.asList(principal.split(AUTH_USERNAME_DOMAIN_SEPARATOR)));

        if (parts.size() > BigInteger.ONE.intValue()) {
            String domain = parts.getLast();
            log.info("Ldap com.icthh.xm.uaa.domain @{} for user {}", domain, principal);

            Optional<AuthenticationProvider> providerOpt = providerBuilder.build(domain);
            if (providerOpt.isPresent()) {
                provider = providerOpt.get();
            }
        }

        return provider;
    }

    /**
     * {@inheritDoc}
     */
    @LogicExtensionPoint(value = "Authenticate")
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication result = getProvider(authentication).authenticate(authentication);
        log.info("authenticated: {}, role: {}, {}",result.isAuthenticated(), result.getAuthorities(), result.getPrincipal());
        DomainUserDetails domainUserDetails = (DomainUserDetails) result.getPrincipal();
        User user = userService.getUser(domainUserDetails.getUserKey());
        LocalDate updatePasswordDate = user.getUpdatePasswordDate().atZone(UTC).toLocalDate();
        Integer expirationPeriod = tenantPropertiesService.getTenantProps().getPasswordExpirationPeriod();
        LocalDate currentDate = LocalDate.now();
        log.info("check password expiration, passwordUpdate: {}, currentDate: {}, expirationPeriod: {}",
            updatePasswordDate, currentDate, expirationPeriod);
        Period period = Period.between(currentDate, updatePasswordDate);
        int days = period.getDays();

        if (days > expirationPeriod && expirationPeriod != -1) {
            throw new BusinessException("Password expiration period is over, please change password");
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return Boolean.TRUE;
    }

}
