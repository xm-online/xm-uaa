package com.icthh.xm.uaa.security.oauth2.otp;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * The {@link OtpGenerator} interface.
 */
public interface OtpGenerator {

    /**
     * Generates OTP for given authentication object.
     *
     * @param authentication the authentication object to generate OTP for
     * @return OTP value
     */
    String generate(OAuth2Authentication authentication);

}
