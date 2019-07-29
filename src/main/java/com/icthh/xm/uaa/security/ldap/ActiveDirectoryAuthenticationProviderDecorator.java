package com.icthh.xm.uaa.security.ldap;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;

@RequiredArgsConstructor
public class ActiveDirectoryAuthenticationProviderDecorator implements AuthenticationProvider {

    private final AuthenticationProvider authenticationProvider;
    private final TenantProperties.Ldap ldap;

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (isNotBlank(ldap.getAuthField())) {
            String username = findUserPrincipalName(authentication.getName());
            Object credentials = authentication.getCredentials();
            return authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(username, credentials));
        } else {
            authenticationProvider.authenticate(authentication);
        }
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return authenticationProvider.supports(aClass);
    }

    @SneakyThrows
    protected String findUserPrincipalName(String username) {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        DirContextOperations dirContextOperations = SpringSecurityLdapTemplate.searchForSingleEntryInternal(
            bindBySystemUser(), searchControls, ldap.getRootDn(), ldap.getSearchFields(),
            new Object[] {username});
        return dirContextOperations.getStringAttribute(ldap.getAuthField());
    }

    @SneakyThrows
    protected DirContext bindBySystemUser() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldap.getSystemUser());
        env.put(Context.PROVIDER_URL, ldap.getProviderUrl());
        env.put(Context.SECURITY_CREDENTIALS, ldap.getSystemPassword());
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.OBJECT_FACTORIES, DefaultDirObjectFactory.class.getName());
        return new InitialLdapContext(env, null);
    }
}
