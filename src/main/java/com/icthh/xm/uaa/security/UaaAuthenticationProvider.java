package com.icthh.xm.uaa.security;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.uaa.config.Constants.AUTH_USERNAME_DOMAIN_SEPARATOR;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.lep.keyresolver.OptionalProfileHeaderResolver;
import com.icthh.xm.uaa.security.ldap.LdapAuthenticationProviderBuilder;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationToken;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@Slf4j
@IgnoreLogginAspect
@LepService(group = "security.provider")
public class UaaAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider defaultProvider;
    private final LdapAuthenticationProviderBuilder providerBuilder;
    private final UserService userService;
    private final TenantPropertiesService tenantPropertiesService;
    @Setter(onMethod = @__(@Autowired))
    private UaaAuthenticationProvider self;

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
    @LogicExtensionPoint(value = "Authenticate", resolver = OptionalProfileHeaderResolver.class)
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication result = getProvider(authentication).authenticate(authentication);
        log.info("authenticated: {}, role: {}, {}", result.isAuthenticated(), result.getAuthorities(), result.getPrincipal());
        checkPasswordExpiration(result);
        checkTermsOfConditions(result);
        return result;
    }

    private void checkTermsOfConditions(Authentication authentication) {
        TenantProperties.PublicSettings publicSettings = tenantPropertiesService.getTenantProps().getPublicSettings();
        if (publicSettings == null ||
            publicSettings.getTermsOfConditionsEnabled() == null ||
            !publicSettings.getTermsOfConditionsEnabled()) {
            return;
        }

        User user = getUser(authentication);
        if (!self.isTermsOfConditionsAccepted(user) && !user.getAuthorities().contains(SUPER_ADMIN)) {
            User userWithUpdatedToken = userService.updateAcceptTermsOfConditionsToken(user);
            throw new NeedTermsOfConditionsException(userWithUpdatedToken.getAcceptTocOneTimeToken());
        }
    }

    @LogicExtensionPoint(value = "IsTermsOfConditionsAccepted")
    public boolean isTermsOfConditionsAccepted(User user) {
        return user.getAcceptTocTime() != null;
    }

    private void checkPasswordExpiration(Authentication authentication) {
        Integer expirationPeriod = tenantPropertiesService.getTenantProps().getSecurity().getPasswordExpirationPeriod();
        if (expirationPeriod > 0) {
            User user = getUser(authentication);
            LocalDate updatePasswordDate = user.getUpdatePasswordDate().atZone(UTC).toLocalDate();
            LocalDate currentDate = LocalDate.now();
            log.info("check password expiration, passwordUpdateDate: {}, currentDate: {}, expirationPeriod: {}",
                updatePasswordDate, currentDate, expirationPeriod);
            long period = DAYS.between(updatePasswordDate, currentDate);
            if (period > expirationPeriod && !user.getAuthorities().contains(SUPER_ADMIN)) {
                throw new CredentialsExpiredException("Password expiration period is over, please change password");
            }
        }
    }

    private User getUser(Authentication authentication) {
        DomainUserDetails domainUserDetails = (DomainUserDetails) authentication.getPrincipal();
        return userService.getUser(domainUserDetails.getUserKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        //TODO - remove log after investigation
        log.info("FORDEBUG: check for supported authentication: {}", authentication);
        if (TfaOtpAuthenticationToken.class.isAssignableFrom(authentication)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}
