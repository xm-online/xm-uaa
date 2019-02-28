package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.config.Constants;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * Implementation of AuditorAware based on Spring Security.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        String currentUserLogin = SecurityUtils.getCurrentUserLogin();
        String userName = currentUserLogin == null ? Constants.SYSTEM_ACCOUNT : currentUserLogin;
        return Optional.of(userName);
    }
}
