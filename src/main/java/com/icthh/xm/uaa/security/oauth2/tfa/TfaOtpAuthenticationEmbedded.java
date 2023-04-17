package com.icthh.xm.uaa.security.oauth2.tfa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * The {@link TfaOtpAuthenticationEmbedded} class.
 */
@Slf4j
public class TfaOtpAuthenticationEmbedded extends TfaOtpAuthenticationProvider {

    public TfaOtpAuthenticationEmbedded(UserDetailsService userDetailsService,
                                        PasswordEncoder otpEncoder) {
        super(userDetailsService, otpEncoder);
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
                log.debug("User '" + username + "' not found");
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
            additionalAuthenticationChecks(tfaOtpAuthentication);
        } catch (AuthenticationException exception) {
            if (!cacheWasUsed) {
                throw exception;
            }

            // There was a problem, so try again after checking
            // we're using latest data (i.e. not from the cache)
            cacheWasUsed = false;
            user = retrieveUser(username, tfaOtpAuthentication);
            preAuthenticationChecks.check(user);
            additionalAuthenticationChecks(tfaOtpAuthentication);
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

    private void additionalAuthenticationChecks(TfaOtpAuthenticationToken authentication) throws AuthenticationException {
        // check is credentials "container object" exist
        TfaOtpAuthenticationToken.OtpCredentials credentials = authentication.getCredentials();
        if (credentials == null) {
            log.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials",
                "Bad credentials"));
        }

        // get credentials fields
        final String presentedOtp = credentials.getOtp();
        String encodedOtp = credentials.getEncodedOtp();

        // check is credentials fields not blank
        if (StringUtils.isBlank(presentedOtp) || StringUtils.isBlank(encodedOtp)) {
            log.debug("Authentication failed: no OTP provided");

            throw new BadCredentialsException(messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials",
                "Bad credentials"));
        }

        // validate OTP
        if (!otpEncoder.matches(presentedOtp, encodedOtp)) {
            log.debug("Authentication failed: password does not match stored value");

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

}
