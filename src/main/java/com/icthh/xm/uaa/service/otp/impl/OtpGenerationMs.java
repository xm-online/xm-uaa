package com.icthh.xm.uaa.service.otp.impl;

import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.service.otp.OtpGenerationStrategy;
import com.icthh.xm.uaa.service.otp.OtpService;
import com.icthh.xm.uaa.service.otp.OtpType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpGenerationMs implements OtpGenerationStrategy {

    private final OtpService otpService;

    @Override
    public OtpType getOtpType() {
        return OtpType.OTP_MS;
    }

    @Override
    public void generateOtp(OAuth2Authentication authentication) {
        Object principal = authentication.getPrincipal();
        DomainUserDetails userDetails = (DomainUserDetails) principal;
        Long otpRequest = otpService.prepareOtpRequest(userDetails);
        userDetails.setOtpId(otpRequest);
    }
}
