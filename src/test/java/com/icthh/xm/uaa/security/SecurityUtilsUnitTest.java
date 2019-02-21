package com.icthh.xm.uaa.security;

import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
* Test class for the SecurityUtils utility class.
*
* @see SecurityUtils
*/
public class SecurityUtilsUnitTest {

    @Test
    public void testgetCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "admin"));
        SecurityContextHolder.setContext(securityContext);
        String login = SecurityUtils.getCurrentUserLogin();
        assertThat(login).isEqualTo("admin");
    }
}
