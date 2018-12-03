package com.icthh.xm.uaa.security.ldap;

import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.icthh.xm.uaa.config.Constants.AUTH_USERNAME_DOMAIN_SEPARATOR;

public class UaaBindAuthenticator extends BindAuthenticator {

    /**
     * {@inheritDoc}
     */
    public UaaBindAuthenticator(BaseLdapPathContextSource contextSource) {
        super(contextSource);
    }

    @Override
    protected List<String> getUserDns(String username) {
        LinkedList<String> parts = new LinkedList<>(Arrays.asList(username.split(AUTH_USERNAME_DOMAIN_SEPARATOR)));
        return super.getUserDns(parts.getFirst());
    }
}
