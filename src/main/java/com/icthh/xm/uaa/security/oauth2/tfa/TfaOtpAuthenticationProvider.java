package com.icthh.xm.uaa.security.oauth2.tfa;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Objects;

/**
 * The {@link TfaOtpAuthenticationProvider} class.
 */
public class TfaOtpAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TfaOtpAuthenticationProvider.class);
    private static final String USER_NOT_FOUND_OTP = "userNotFoundOtp";

    private final UserDetailsService userDetailsService;
    private PasswordEncoder otpEncoder;
    private String userNotFoundEncodedOtp;

    private boolean hideUserNotFoundExceptions = true;
    private boolean forcePrincipalAsString = false;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    private UserCache userCache = new NullUserCache();
    private UserDetailsChecker preAuthenticationChecks = new DefaultPreAuthenticationChecks();
    private UserDetailsChecker postAuthenticationChecks = new DefaultPostAuthenticationChecks();
    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    /**
     * Constructor.
     *
     * @param userDetailsService user details service
     * @param otpEncoder the OTP encoder
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

    @Override
    public boolean supports(Class<?> authentication) {
        return TfaOtpAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(TfaOtpAuthenticationToken.class, authentication,
                            "Only TfaOtpAuthenticationToken is supported");

        final TfaOtpAuthenticationToken tfaOtpAuthentication = TfaOtpAuthenticationToken.class.cast(authentication);

        // Determine username
        final String username = (tfaOtpAuthentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();

        // retrieve user
        boolean cacheWasUsed = true;
        UserDetails user = this.userCache.getUserFromCache(username);
        if (user == null) {
            cacheWasUsed = false;

            try {
                user = retrieveUser(username, tfaOtpAuthentication);
            } catch (UsernameNotFoundException notFound) {
                LOGGER.debug("User '" + username + "' not found");
                throw isHideUserNotFoundExceptions()
                    ? new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"))
                    : notFound;
            }

            Assert.notNull(user,
                           "retrieveUser returned null - a violation of the interface contract");
        }

        // validate user/account
        try {
            preAuthenticationChecks.check(user);
            additionalAuthenticationChecks(user, tfaOtpAuthentication);
        } catch (AuthenticationException exception) {
            if (!cacheWasUsed) {
                throw exception;
            }

            // There was a problem, so try again after checking
            // we're using latest data (i.e. not from the cache)
            cacheWasUsed = false;
            user = retrieveUser(username, tfaOtpAuthentication);
            preAuthenticationChecks.check(user);
            additionalAuthenticationChecks(user, tfaOtpAuthentication);
        }

        postAuthenticationChecks.check(user);

        if (!cacheWasUsed) {
            this.userCache.putUserInCache(user);
        }

        Object principalToReturn = user;
        if (forcePrincipalAsString) {
            principalToReturn = user.getUsername();
        }
        return createSuccessAuthentication(principalToReturn, tfaOtpAuthentication, user);
    }

    private UserDetails retrieveUser(String username, TfaOtpAuthenticationToken tfaOtpAuthentication) {
        UserDetails loadedUser;

        try {
            loadedUser = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException notFound) {
            if (tfaOtpAuthentication.getCredentials() != null) {
                String presentedOtp = tfaOtpAuthentication.getCredentials().getOtp();
                otpEncoder.matches(presentedOtp, userNotFoundEncodedOtp);
            }
            throw notFound;
        } catch (Exception repositoryProblem) {
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }

        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException(
                "UserDetailsService returned null, which is an interface contract violation");
        }
        return loadedUser;
    }

    private void additionalAuthenticationChecks(UserDetails userDetails,
                                                TfaOtpAuthenticationToken authentication) throws AuthenticationException {
        // check is credentials "container object" exist
        TfaOtpAuthenticationToken.OtpCredentials credentials = authentication.getCredentials();
        if (credentials == null) {
            LOGGER.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials",
                "Bad credentials"));
        }

        // get credentials fields
        final String presentedOtp = credentials.getOtp();
        final String encodedOtp = credentials.getEncodedOtp();

        // check is credentials fields not blank
        if (StringUtils.isBlank(presentedOtp) || StringUtils.isBlank(encodedOtp)) {
            LOGGER.debug("Authentication failed: no OTP provided");

            throw new BadCredentialsException(messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials",
                "Bad credentials"));
        }

        // validate OTP
        if (!otpEncoder.matches(presentedOtp, encodedOtp)) {
            LOGGER.debug("Authentication failed: password does not match stored value");

            throw new BadCredentialsException(messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials",
                "Bad credentials"));
        }
    }

    private Authentication createSuccessAuthentication(Object principal,
                                                       TfaOtpAuthenticationToken authentication,
                                                       UserDetails user) {
        // Ensure we return the original credentials the user supplied,
        // so subsequent attempts are successful even with encoded passwords.
        // Also ensure we return the original getDetails(), so that future
        // authentication events after cache expiry contain the details
        Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapAuthorities(user.getAuthorities());
        TfaOtpAuthenticationToken result = new TfaOtpAuthenticationToken(principal,
                                                                         authentication.getCredentials(),
                                                                         authorities);
        result.setDetails(authentication.getDetails());

        return result;
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
