package com.icthh.xm.uaa.security.oauth2.tfa;

import com.icthh.xm.uaa.service.otp.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * The {@link TfaOtpAuthenticationOtpMs} class.
 */
@Slf4j
@Service
public class TfaOtpAuthenticationOtpMs extends TfaOtpAuthenticationProvider {

    private final OtpService otpService;

    public TfaOtpAuthenticationOtpMs(UserDetailsService userDetailsService,
                                     PasswordEncoder otpEncoder,
                                     OtpService otpService) {
        super(userDetailsService, otpEncoder);
        this.otpService = otpService;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TfaOtpMsAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(TfaOtpMsAuthenticationToken.class, authentication,
            "Only TfaOtpMsAuthenticationToken is supported");

        final TfaOtpMsAuthenticationToken tfaOtpMsAuthentication = TfaOtpMsAuthenticationToken.class.cast(authentication);

        // Determine username
        final String username = (tfaOtpMsAuthentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();

        // retrieve user
        boolean cacheWasUsed = true;
        UserDetails user = this.userCache.getUserFromCache(username);
        if (user == null) {
            cacheWasUsed = false;

            try {
                user = retrieveUser(username);
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
            additionalAuthenticationChecks(tfaOtpMsAuthentication);
        } catch (AuthenticationException exception) {
            if (!cacheWasUsed) {
                throw exception;
            }

            // There was a problem, so try again after checking
            // we're using latest data (i.e. not from the cache)
            cacheWasUsed = false;
            user = retrieveUser(username);
            preAuthenticationChecks.check(user);
            additionalAuthenticationChecks(tfaOtpMsAuthentication);
        }

        postAuthenticationChecks.check(user);

        if (!cacheWasUsed) {
            this.userCache.putUserInCache(user);
        }

        Object principalToReturn = user;
        if (forcePrincipalAsString) {
            principalToReturn = user.getUsername();
        }
        return createSuccessAuthentication(principalToReturn, tfaOtpMsAuthentication, user);
    }

    private UserDetails retrieveUser(String username) {
        UserDetails loadedUser = userDetailsService.loadUserByUsername(username);

        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException(
                "UserDetailsService returned null, which is an interface contract violation");
        }
        return loadedUser;
    }

    private void additionalAuthenticationChecks(TfaOtpMsAuthenticationToken authentication) throws AuthenticationException {
        // check is credentials "container object" exist
        TfaOtpMsAuthenticationToken.OtpMsCredentials credentials = authentication.getCredentials();
        if (credentials == null) {
            log.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials",
                "Bad credentials"));
        }

        // get credentials fields
        final String presentedOtp = credentials.getOtp();
        Long otpId = credentials.getOtpId();
        boolean isValidOtp = otpService.checkOtpRequest(otpId, presentedOtp);
        if (!isValidOtp) {
            log.debug("Authentication failed: no OTP MS provided");

            throw new BadCredentialsException(messages.getMessage(
                "AbstractUserDetailsAuthenticationProvider.badCredentials",
                "Bad credentials"));
        }

    }

    private Authentication createSuccessAuthentication(Object principal,
                                                       TfaOtpMsAuthenticationToken authentication,
                                                       UserDetails user) {
        // Ensure we return the original credentials the user supplied,
        // so subsequent attempts are successful even with encoded passwords.
        // Also ensure we return the original getDetails(), so that future
        // authentication events after cache expiry contain the details
        Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapAuthorities(user.getAuthorities());
        TfaOtpMsAuthenticationToken result = new TfaOtpMsAuthenticationToken(principal,
            authentication.getCredentials(),
            authorities);
        result.setDetails(authentication.getDetails());

        return result;
    }

}
