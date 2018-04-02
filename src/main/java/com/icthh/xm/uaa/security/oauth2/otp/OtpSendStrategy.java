package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.uaa.security.DomainUserDetails;

/**
 * The {@link OtpSendStrategy} class.
 */
public interface OtpSendStrategy {

    void send(String otp, DomainUserDetails userDetails);

}
