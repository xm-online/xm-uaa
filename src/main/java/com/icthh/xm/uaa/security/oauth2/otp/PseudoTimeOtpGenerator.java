package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.uaa.security.DomainUserDetails;
import org.jboss.aerogear.security.otp.Totp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.Objects;

/**
 * The {@link PseudoTimeOtpGenerator} class.
 */
public class PseudoTimeOtpGenerator implements OtpGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PseudoTimeOtpGenerator.class);

    @Override
    public String generate(OAuth2Authentication authentication) {
        DomainUserDetails userDetails = getDomainUserDetails(authentication);
        Totp totp = new Totp(userDetails.getTfaOtpSecret());
        String otp = totp.now();

        LOGGER.debug("OTP: {} generated for user-key: {}", otp, userDetails.getUserKey());
        return otp;
    }

    private static DomainUserDetails getDomainUserDetails(OAuth2Authentication authentication) {
        Object principal = authentication.getPrincipal();
        Objects.requireNonNull(principal, "principal can't be null");
        if (!(principal instanceof DomainUserDetails)) {
            throw new IllegalArgumentException("Unsupported authentication principal type, expected: "
                                                   + DomainUserDetails.class.getSimpleName() + ", actual: "
                                                   + principal.getClass().getCanonicalName()
            );
        }

        return DomainUserDetails.class.cast(principal);
    }

}
