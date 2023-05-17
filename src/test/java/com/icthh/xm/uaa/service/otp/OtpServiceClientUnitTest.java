package com.icthh.xm.uaa.service.otp;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.uaa.service.otp.OtpService.OneTimePasswordDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import static com.icthh.xm.uaa.service.otp.ReceiverTypeKey.PHONE_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;


@RunWith(MockitoJUnitRunner.class)
public class OtpServiceClientUnitTest {

    private static final String TFA_OTP_TYPE_KEY = "2FACTOR-AD-OTP-SIGN-IN";
    private static final String DESTINATION_PHONE = "+380663127876";
    private static final Long OTP_ID = 1L;
    private static final String OTP = "otp";
    private static final String LANG_KEY = "en";
    private static final String GENERATE_OTP_URL = "http://otp-app/api/one-time-password";
    private static final String CHECK_OTP_URL = "http://otp-app/api/one-time-password/check";

    @Mock
    private OAuth2RestTemplate oAuth2RestTemplate;
    @InjectMocks
    private OtpServiceClient otpServiceClient;

    @Test
    public void createOtp() {
        OneTimePasswordDto oneTimePasswordDto = buildOneTimePasswordDto(null, PHONE_NUMBER.getValue());
        HttpEntity<OneTimePasswordDto> httpEntity = new HttpEntity<>(oneTimePasswordDto, buildHeaders());
        ResponseEntity<OneTimePasswordDto> response = ResponseEntity
            .status(HttpStatus.OK)
            .body(buildOneTimePasswordDto(OTP_ID, PHONE_NUMBER.getValue()));
        doReturn(response).when(oAuth2RestTemplate).exchange(any(String.class), any(), any(), eq(OneTimePasswordDto.class));

        Long actualOtpId = otpServiceClient.createOtp(GENERATE_OTP_URL, oneTimePasswordDto);

        verify(oAuth2RestTemplate).exchange(eq(GENERATE_OTP_URL), eq(POST), eq(httpEntity), eq(OneTimePasswordDto.class));
        assertEquals(OTP_ID, actualOtpId);
    }

    @Test
    public void createOtp_throw_exception() {
        OneTimePasswordDto oneTimePasswordDto = buildOneTimePasswordDto(null, PHONE_NUMBER.getValue());
        when(oAuth2RestTemplate.exchange(any(String.class), any(), any(), eq(OneTimePasswordDto.class))).thenThrow(new RuntimeException());

        Throwable thrown = catchThrowable(() -> otpServiceClient.createOtp(GENERATE_OTP_URL, oneTimePasswordDto));

        assertThat(thrown).isInstanceOf(BusinessException.class);
    }

    @Test
    public void checkOtp() {
        OtpService.OneTimePasswordCheckDto oneTimePasswordCheckDto = buildOneTimePasswordCheckDto();
        HttpEntity<OtpService.OneTimePasswordCheckDto> httpEntity = new HttpEntity<>(oneTimePasswordCheckDto, buildHeaders());
        ResponseEntity<OneTimePasswordDto> response = ResponseEntity.status(HttpStatus.OK).body(new OneTimePasswordDto());
        doReturn(response).when(oAuth2RestTemplate).exchange(any(String.class), any(), any(), eq(Boolean.class));

        boolean result = otpServiceClient.checkOtp(CHECK_OTP_URL, oneTimePasswordCheckDto);

        verify(oAuth2RestTemplate).exchange(eq(CHECK_OTP_URL), eq(POST), eq(httpEntity), eq(OneTimePasswordDto.class));
        assertTrue(result);
    }

    @Test
    public void checkOtp_throw_exception() {
        OtpService.OneTimePasswordCheckDto oneTimePasswordCheckDto = buildOneTimePasswordCheckDto();
        when(oAuth2RestTemplate.exchange(any(String.class), any(), any(), eq(OneTimePasswordDto.class))).thenThrow(new RuntimeException());

        boolean result = otpServiceClient.checkOtp(CHECK_OTP_URL, oneTimePasswordCheckDto);

        assertFalse(result);
    }

    private OneTimePasswordDto buildOneTimePasswordDto(Long id, String receiverTypeKey) {
        return new OneTimePasswordDto(id, DESTINATION_PHONE, receiverTypeKey, TFA_OTP_TYPE_KEY, LANG_KEY);
    }

    private OtpService.OneTimePasswordCheckDto buildOneTimePasswordCheckDto() {
        return new OtpService.OneTimePasswordCheckDto(OTP_ID, OTP);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

}
