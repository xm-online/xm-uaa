package com.icthh.xm.uaa.security.provider;

import com.icthh.xm.uaa.security.ImpersonateAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

@Slf4j
public class UaaDaoAuthenticationProvider extends DaoAuthenticationProvider {
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication instanceof ImpersonateAuthenticationToken) {
            log.info("Skip password check for impersonate login to {}", authentication.getPrincipal());
            return;
        }
        super.additionalAuthenticationChecks(userDetails, authentication);
    }
}
