package com.icthh.xm.uaa.security.ldap;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

public class UaaLdapAuthenticationProvider extends LdapAuthenticationProvider {

    public UaaLdapAuthenticationProvider(LdapAuthenticator authenticator,
                                         LdapAuthoritiesPopulator authoritiesPopulator) {
        super(authenticator, authoritiesPopulator);
    }

    //todo add logic for new user
    @Override
    protected Authentication createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication,
                                                            UserDetails user) {
        return super.createSuccessfulAuthentication(authentication, user);
    }
}
