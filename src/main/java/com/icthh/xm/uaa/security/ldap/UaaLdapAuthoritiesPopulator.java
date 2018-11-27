package com.icthh.xm.uaa.security.ldap;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UaaLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    private TenantProperties.Ldap ldapConf;

    public UaaLdapAuthoritiesPopulator(TenantProperties.Ldap ldapConf) {
        this.ldapConf = ldapConf;
    }

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData,
                                                                        String username) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(ldapConf.getRole().getDefaultRole()));
        return authorities;
    }
}
