package com.icthh.xm.uaa.security.ldap;

import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class UaaBindAuthenticator extends BindAuthenticator {

    /**
     * {@inheritDoc}
     */
    public UaaBindAuthenticator(BaseLdapPathContextSource contextSource) {
        super(contextSource);
    }

    @Override
    protected List<String> getUserDns(String username) {
        LinkedList<String> parts = new LinkedList<>(Arrays.asList(username.split("@")));
        return super.getUserDns(parts.getFirst());
    }
}
