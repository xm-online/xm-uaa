package com.icthh.xm.uaa.security.ldap;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Ldap;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.stereotype.Component;

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
        return Optional.of(buildAuthenticationProvider(conf));
    }

    private AuthenticationProvider buildAuthenticationProvider(Ldap conf) {
        Type ldapType = conf.getType();
        log.info("Ldap type {}", ldapType);
        if (conf.getType() == null) {
            ldapType = Type.OPEN_LDAP;
        }

        AuthenticationProvider provider = null;
        switch (ldapType) {
            case ACTIVE_DIRECTORY:
                provider = buildAdAuthProvier(conf);
                break;
            case OPEN_LDAP:
                provider = buildLdapAuthProvider(conf);
                break;
        }

        return provider;
    }

    private AuthenticationProvider buildLdapAuthProvider(Ldap conf) {
        //ldap context which used for role searching
        DefaultSpringSecurityContextSource ctx = new DefaultSpringSecurityContextSource(conf.getProviderUrl());
        ctx.setUserDn(conf.getSystemUser());
        ctx.setPassword(conf.getSystemPassword());
        ctx.afterPropertiesSet();

        //role extractor
        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
            new DefaultLdapAuthoritiesPopulator(ctx, conf.getGroupSearchBase());
        authoritiesPopulator.setSearchSubtree(conf.getGroupSearchSubtree());
        authoritiesPopulator.setRolePrefix(StringUtils.EMPTY);
        authoritiesPopulator.setConvertToUpperCase(Boolean.FALSE);

        //bind authenticator for password checking
        BindAuthenticator bindAuthenticator = new UaaBindAuthenticator(ctx);
        bindAuthenticator.setUserDnPatterns(Stream.of(conf.getUserDnPattern()).toArray(String[]::new));

        //create spring ldap authenticator provider
        LdapAuthenticationProvider ldapAuthenticationProvider =
            new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);
        ldapAuthenticationProvider.setUserDetailsContextMapper(
            new UaaLdapUserDetailsContextMapper(userDetailsService, userService, conf));

        return new CutDomainAuthenticationProviderDecorator(ldapAuthenticationProvider, conf);
    }

    private AuthenticationProvider buildAdAuthProvier(Ldap conf) {
        ActiveDirectoryLdapAuthenticationProvider adLdapAuthenticationProvider
            = new ActiveDirectoryLdapAuthenticationProvider(null, conf.getProviderUrl(), conf.getRootDn());

        adLdapAuthenticationProvider.setUserDetailsContextMapper(
            new UaaLdapUserDetailsContextMapper(userDetailsService, userService, conf));

        if (isNotBlank(conf.getSearchFields())) {
            adLdapAuthenticationProvider.setSearchFilter(conf.getSearchFields());
        }

        return new CutDomainAuthenticationProviderDecorator(
            new ActiveDirectoryAuthenticationProviderDecorator(adLdapAuthenticationProvider, conf), conf);
    }
}
