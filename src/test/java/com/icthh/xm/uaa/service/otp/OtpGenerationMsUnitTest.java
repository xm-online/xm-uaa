package com.icthh.xm.uaa.service.otp;

import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.service.otp.impl.OtpGenerationMs;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OtpGenerationMsUnitTest {

    private static final Long OTP_ID = 1L;

    @Mock
    private OtpService otpService;
    @InjectMocks
    private OtpGenerationMs otpGenerationMs;

    @Test
    public void generateOtp() {
        DomainUserDetails userDetails = buildDomainUserDetails();
        OAuth2Authentication authentication = createAuthentication(userDetails);
        when(otpService.prepareOtpRequest(userDetails)).thenReturn(OTP_ID);

        otpGenerationMs.generateOtp(authentication);

        verify(otpService).prepareOtpRequest(ArgumentMatchers.eq(userDetails));
        Assert.assertEquals(userDetails.getOtpId(), OTP_ID);
    }

    @Test
    public void getOtpType() {
        OtpType otpType = otpGenerationMs.getOtpType();

        Assert.assertEquals(OtpType.OTP_MS, otpType);
    }

    private DomainUserDetails buildDomainUserDetails() {
        return new DomainUserDetails(
            "aroze",
            "password",
            emptyList(),
            "",
            "",
            true,
            "test",
            null,
            null,
            null,
            null,
            false,
            null,
            emptyList(),
            ""
        );
    }

    private OAuth2Authentication createAuthentication(DomainUserDetails principal) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());

        OAuth2Request authRequest = new OAuth2Request(null, "testClient", null,
            true, emptySet(), null, null, null, null);
        return new OAuth2Authentication(authRequest, authentication);
    }

}
