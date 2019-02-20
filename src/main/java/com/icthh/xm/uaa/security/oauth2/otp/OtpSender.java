package com.icthh.xm.uaa.security.oauth2.otp;

/**
 * The {@link OtpSender} interface.
 */
public interface OtpSender {

    void send(String otp, String destination, String userKey);

}
