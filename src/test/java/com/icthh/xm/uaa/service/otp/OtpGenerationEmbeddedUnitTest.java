package com.icthh.xm.uaa.service.otp;

import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.oauth2.otp.OtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSendStrategy;
import com.icthh.xm.uaa.security.oauth2.otp.OtpStore;
import com.icthh.xm.uaa.service.otp.impl.OtpGenerationEmbedded;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OtpGenerationEmbeddedUnitTest {

    private static final String OTP = "otp";

    @Mock
    private OtpGenerator otpGenerator;
    @Mock
    private OtpSendStrategy otpSendStrategy;
    @Mock
    private OtpStore otpStore;
    @InjectMocks
    private OtpGenerationEmbedded otpGenerationEmbedded;

    @Test
    public void getOtpType() {
        OtpType otpType = otpGenerationEmbedded.getOtpType();

        Assert.assertEquals(OtpType.EMBEDDED, otpType);
    }

    @Test
    public void generateOtp() {
        DomainUserDetails userDetails = buildDomainUserDetails();
        OAuth2Authentication authentication = createAuthentication(userDetails);
        when(otpGenerator.generate(authentication)).thenReturn(OTP);

        otpGenerationEmbedded.generateOtp(authentication);

        verify(otpGenerator).generate(ArgumentMatchers.eq(authentication));
        verify(otpStore).storeOtp(ArgumentMatchers.eq(OTP), ArgumentMatchers.eq(authentication));
        verify(otpSendStrategy).send(ArgumentMatchers.eq(OTP), ArgumentMatchers.eq(userDetails));
    }

    @Test
    public void generateOtp_authenticated_false() {
        OAuth2Authentication authentication = createWrongAuthentication(false, null);

        otpGenerationEmbedded.generateOtp(authentication);

        verify(otpGenerator, times(0)).generate(any());
        verify(otpStore, times(0)).storeOtp(any(), any());
        verify(otpSendStrategy, times(0)).send(any(), any());
    }

    @Test
    public void generateOtp_without_principle() {
        OAuth2Authentication authentication = createWrongAuthentication(true, null);

        otpGenerationEmbedded.generateOtp(authentication);

        verify(otpGenerator, times(0)).generate(any());
        verify(otpStore, times(0)).storeOtp(any(), any());
        verify(otpSendStrategy, times(0)).send(any(), any());
    }

    private DomainUserDetails buildDomainUserDetails() {
        return new DomainUserDetails(
            "aroze",
            "password",
            emptyList(),
            "",
            "",
            null,
            null,
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
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
            principal.getAuthorities());

        OAuth2Request authRequest = new OAuth2Request(null, "testClient", null,
            true, emptySet(), null, null, null, null);

        return new OAuth2Authentication(authRequest, authentication);
    }

    private OAuth2Authentication createWrongAuthentication(boolean authenticated, DomainUserDetails principal) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, "", emptyList());
        if (!authenticated) {
            authentication.setAuthenticated(authenticated);
        }

        OAuth2Request authRequest = new OAuth2Request(null, "testClient", null,
            true, emptySet(), null, null, null, null);

        return new OAuth2Authentication(authRequest, authentication);
    }

}
