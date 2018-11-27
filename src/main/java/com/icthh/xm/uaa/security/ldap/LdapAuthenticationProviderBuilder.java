package com.icthh.xm.uaa.security.ldap;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Ldap;
import com.icthh.xm.uaa.service.TenantPropertiesService;
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
    private final UaaLdapUserDetailsContextMapper ldapUserDetailsContextMapper;

    public Optional<AuthenticationProvider> build(String domain) {
        List<Ldap> ldapList = tenantPropertiesService.getTenantProps().getLdap();
        Optional<Ldap> ldapOpt = ldapList.stream().filter(l -> l.getDomain().equals(domain)).findFirst();

        if (!ldapOpt.isPresent()) {
            log.info("Ldap configuration not found for domain {}", domain);
            return Optional.empty();
        }

        Ldap conf = ldapOpt.get();

        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
            conf.getProviderUrl());
        contextSource.afterPropertiesSet();

        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        bindAuthenticator.setUserDnPatterns(Stream.of(conf.getUserDnPattern()).toArray(String[]::new));

        LdapAuthenticationProvider ldapAuthenticationProvider =
            new LdapAuthenticationProvider(bindAuthenticator, new UaaLdapAuthoritiesPopulator(conf));

        ldapAuthenticationProvider.setUserDetailsContextMapper(ldapUserDetailsContextMapper);

        return Optional.of(ldapAuthenticationProvider);
    }
}
