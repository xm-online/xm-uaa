package com.icthh.xm.uaa.security.ldap;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Ldap;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
@Slf4j
@Component
public class LdapAuthenticationProviderBuilder {

    private final TenantPropertiesService tenantPropertiesService;
    private final DomainUserDetailsService userDetailsService;
    private final UserService userService;

    public Optional<AuthenticationProvider> build(String domain) {
        List<Ldap> ldapList = tenantPropertiesService.getTenantProps().getLdap();

        //get tenant ldap configuration
        Optional<Ldap> ldapOpt = ldapList.stream().filter(l -> l.getDomain().equals(domain)).findFirst();
        if (!ldapOpt.isPresent()) {
            log.info("Ldap configuration not found for domain {}", domain);
            return Optional.empty();
        }
        Ldap conf = ldapOpt.get();

        //build spring ldap context
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
            conf.getProviderUrl());
        contextSource.afterPropertiesSet();

        //create spring ldap authenticator
        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        bindAuthenticator.setUserDnPatterns(Stream.of(conf.getUserDnPattern()).toArray(String[]::new));

        //create spring ldap authenticator provider
        LdapAuthenticationProvider ldapAuthenticationProvider =
            new LdapAuthenticationProvider(bindAuthenticator, new UaaLdapAuthoritiesPopulator(conf));
        ldapAuthenticationProvider.setUserDetailsContextMapper(
            new UaaLdapUserDetailsContextMapper(userDetailsService, userService, conf));

        return Optional.of(ldapAuthenticationProvider);
    }
}
