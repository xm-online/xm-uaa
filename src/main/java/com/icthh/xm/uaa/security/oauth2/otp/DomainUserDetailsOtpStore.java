package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.uaa.security.DomainUserDetails;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;

import static com.icthh.xm.uaa.config.Constants.REQ_ATTR_TFA_VERIFICATION_OTP_KEY;

/**
 * The {@link DomainUserDetailsOtpStore} class.
 */
public class DomainUserDetailsOtpStore implements OtpStore {

    private final PasswordEncoder otpEncoder;

    public DomainUserDetailsOtpStore(PasswordEncoder otpEncoder) {
        super();
        this.otpEncoder = Objects.requireNonNull(otpEncoder, "otpEncoder can't be null");
    }

    @Override
    public String storeOtp(String otp, OAuth2Authentication authentication) {
        DomainUserDetails userDetails = getDomainUserDetails(authentication);
        String encodedOtp = otpEncoder.encode(otp);

        // Enrich user details with encoded OTP (User additional details will be copied to JWT access token)
        userDetails.setTfaEncodedOtp(encodedOtp);
        // Enrich request context with encoded OTP (it will indicate to add OTP 'required' into response HTTP header)
        RequestContextHolder.getRequestAttributes().setAttribute(REQ_ATTR_TFA_VERIFICATION_OTP_KEY, encodedOtp,
                                                                 RequestAttributes.SCOPE_REQUEST);
        return encodedOtp;
    }

    private static DomainUserDetails getDomainUserDetails(OAuth2Authentication authentication) {
        Object principal = authentication.getPrincipal();
        Objects.requireNonNull(principal, "principal can't be null");
        if (!(principal instanceof DomainUserDetails)) {
            throw new BadCredentialsException("Unsupported authentication principal type, expected: "
                                                  + DomainUserDetails.class.getSimpleName() + ", actual: "
                                                  + principal.getClass().getCanonicalName()
            );
        }

        return DomainUserDetails.class.cast(principal);
    }

}
