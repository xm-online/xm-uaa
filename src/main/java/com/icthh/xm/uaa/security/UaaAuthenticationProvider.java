package com.icthh.xm.uaa.security;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@Slf4j
@LepService(group = "security.provider")
public class UaaAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider defaultProvider;

    public UaaAuthenticationProvider(@Qualifier("daoAuthenticationProvider") @Lazy
                                         AuthenticationProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    private AuthenticationProvider getProvider() {
        return defaultProvider;
    }

    /**
     * {@inheritDoc}
     */
    @LogicExtensionPoint(value = "Authenticate")
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return getProvider().authenticate(authentication);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return getProvider().supports(authentication);
    }

}
