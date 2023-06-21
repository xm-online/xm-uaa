package com.icthh.xm.uaa.security.oauth2.tfa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * The {@link TfaOtpAuthenticationProvider} class.
 */
public abstract class TfaOtpAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TfaOtpAuthenticationProvider.class);
    private static final String USER_NOT_FOUND_OTP = "userNotFoundOtp";

    final UserDetailsService userDetailsService;
    PasswordEncoder otpEncoder;
    String userNotFoundEncodedOtp;

    boolean hideUserNotFoundExceptions = true;
    boolean forcePrincipalAsString = false;

    GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    UserCache userCache = new NullUserCache();
    UserDetailsChecker preAuthenticationChecks = new DefaultPreAuthenticationChecks();
    UserDetailsChecker postAuthenticationChecks = new DefaultPostAuthenticationChecks();
    MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    /**
     * Constructor.
     *
     * @param userDetailsService user details service
     * @param otpEncoder         the OTP encoder
     */
    public TfaOtpAuthenticationProvider(UserDetailsService userDetailsService,
                                        PasswordEncoder otpEncoder) {
        this.userDetailsService = Objects.requireNonNull(userDetailsService, "userDetailsService can't be null");
        setOtpEncoder(otpEncoder);
    }

    private void setOtpEncoder(PasswordEncoder otpEncoder) {
        Assert.notNull(otpEncoder, "otpEncoder cannot be null");

        this.userNotFoundEncodedOtp = otpEncoder.encode(USER_NOT_FOUND_OTP);
        this.otpEncoder = otpEncoder;
    }

    @PostConstruct
    public final void afterPropertiesSet() {
        Assert.notNull(this.userCache, "A user cache must be set");
        Assert.notNull(this.messages, "A message source must be set");
        Assert.notNull(this.userDetailsService, "userDetailsService must be set");
        Assert.notNull(this.otpEncoder, "otpEncoder must be set");
        Assert.notNull(this.preAuthenticationChecks, "preAuthenticationChecks must be set");
        Assert.notNull(this.postAuthenticationChecks, "postAuthenticationChecks must be set");
        Assert.notNull(this.authoritiesMapper, "authoritiesMapper must be set");
    }

    public boolean isHideUserNotFoundExceptions() {
        return hideUserNotFoundExceptions;
    }

    /**
     * By default {@link TfaOtpAuthenticationProvider} throws a
     * <code>BadCredentialsException</code> if a username is not found or the password is
     * incorrect. Setting this property to <code>false</code> will cause
     * <code>UsernameNotFoundException</code>s to be thrown instead for the former. Note
     * this is considered less secure than throwing <code>BadCredentialsException</code>
     * for both exceptions.
     *
     * @param hideUserNotFoundExceptions set to <code>false</code> if you wish
     *                                   <code>UsernameNotFoundException</code>s to be thrown instead of the non-specific
     *                                   <code>BadCredentialsException</code> (defaults to <code>true</code>)
     */
    public void setHideUserNotFoundExceptions(boolean hideUserNotFoundExceptions) {
        this.hideUserNotFoundExceptions = hideUserNotFoundExceptions;
    }

    public boolean isForcePrincipalAsString() {
        return forcePrincipalAsString;
    }

    public void setForcePrincipalAsString(boolean forcePrincipalAsString) {
        this.forcePrincipalAsString = forcePrincipalAsString;
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }

    public void setUserCache(UserCache userCache) {
        this.userCache = userCache;
    }

    public void setMessages(MessageSourceAccessor messages) {
        this.messages = messages;
    }

    public void setPreAuthenticationChecks(UserDetailsChecker preAuthenticationChecks) {
        this.preAuthenticationChecks = preAuthenticationChecks;
    }

    public void setPostAuthenticationChecks(UserDetailsChecker postAuthenticationChecks) {
        this.postAuthenticationChecks = postAuthenticationChecks;
    }

    /////

    private class DefaultPreAuthenticationChecks implements UserDetailsChecker {
        public void check(UserDetails user) {
            if (!user.isAccountNonLocked()) {
                LOGGER.debug("User account is locked");

                throw new LockedException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.locked",
                    "User account is locked"));
            }

            if (!user.isEnabled()) {
                LOGGER.debug("User account is disabled");

                throw new DisabledException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.disabled",
                    "User is disabled"));
            }

            if (!user.isAccountNonExpired()) {
                LOGGER.debug("User account is expired");

                throw new AccountExpiredException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.expired",
                    "User account has expired"));
            }
        }
    }

    private class DefaultPostAuthenticationChecks implements UserDetailsChecker {
        public void check(UserDetails user) {
            if (!user.isCredentialsNonExpired()) {
                LOGGER.debug("User account credentials have expired");

                throw new CredentialsExpiredException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.credentialsExpired",
                    "User credentials have expired"));
            }
        }
    }

}
