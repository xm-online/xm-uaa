package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.config.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationProviderResolver implements AuthenticationProvider {

    private final AuthenticationProvider defaultProvider;

    public AuthenticationProviderResolver(
        @Qualifier("daoAuthenticationProvider") @Lazy AuthenticationProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    private AuthenticationProvider getProvider() {
        return defaultProvider;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return getProvider().authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return getProvider().supports(authentication);
    }

}
