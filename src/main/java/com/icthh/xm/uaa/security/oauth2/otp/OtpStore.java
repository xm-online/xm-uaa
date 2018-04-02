package com.icthh.xm.uaa.security.oauth2.otp;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * The {@link OtpStore} interface.
 */
public interface OtpStore {

    String storeOtp(String otp, OAuth2Authentication authentication);

}
