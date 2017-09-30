package com.icthh.xm.uaa.security;


import com.icthh.xm.uaa.config.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationRefreshProviderResolver implements AuthenticationRefreshProvider {

    private final AuthenticationRefreshProvider defaultProvider;

    public AuthenticationRefreshProviderResolver(
        @Lazy @Qualifier("defaultAuthenticationRefreshProvider") AuthenticationRefreshProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    private AuthenticationRefreshProvider getProvider() {
        return defaultProvider;
    }

    @Override
    public Authentication refresh(OAuth2Authentication authentication) throws AuthenticationException {
        AuthenticationRefreshProvider provider = getProvider();
        return provider == null ? null : provider.refresh(authentication);
    }

}
