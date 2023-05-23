package com.icthh.xm.uaa.service.otp.impl;

import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.oauth2.otp.OtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSendStrategy;
import com.icthh.xm.uaa.security.oauth2.otp.OtpStore;
import com.icthh.xm.uaa.service.otp.OtpGenerationStrategy;
import com.icthh.xm.uaa.service.otp.OtpType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpGenerationEmbedded implements OtpGenerationStrategy {

    private final OtpGenerator otpGenerator;
    private final OtpSendStrategy otpSendStrategy;
    private final OtpStore otpStore;

    @Override
    public OtpType getOtpType() {
        return OtpType.EMBEDDED;
    }

    @Override
    public void generateOtp(OAuth2Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!authentication.isAuthenticated() || !(principal instanceof DomainUserDetails)) {
            // should't happen but check for sure
            return;
        }

        DomainUserDetails userDetails = DomainUserDetails.class.cast(principal);

        String otp = otpGenerator.generate(authentication);
        otpStore.storeOtp(otp, authentication);
        otpSendStrategy.send(otp, userDetails);
    }
}
