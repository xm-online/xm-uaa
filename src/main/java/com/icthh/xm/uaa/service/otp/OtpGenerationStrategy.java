package com.icthh.xm.uaa.service.otp;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

public interface OtpGenerationStrategy {

    OtpType getOtpType();

    void generateOtp(OAuth2Authentication authentication);
}
