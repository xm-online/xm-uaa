package com.icthh.xm.uaa.service.otp;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.UserLoginDto;
import com.icthh.xm.uaa.service.otp.OtpService.OneTimePasswordCheckDto;
import com.icthh.xm.uaa.service.otp.OtpService.OneTimePasswordDto;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_OTP_RECEIVER_TYPE_KEY;
import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_OTP_TYPE_KEY;
import static com.icthh.xm.uaa.service.otp.ReceiverTypeKey.EMAIL;
import static com.icthh.xm.uaa.service.otp.ReceiverTypeKey.NAME;
import static com.icthh.xm.uaa.service.otp.ReceiverTypeKey.PHONE_NUMBER;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OtpServiceUnitTest {

    private static final String TENANT = "testTenant";
    private static final String TFA_OTP_TYPE_KEY = "2FACTOR-AD-OTP-SIGN-IN";
    private static final String TFA_OTP_TYPE_KEY_CUSTOM = "2FACTOR-AD-OTP-SIGN-IN-CUSTOM";
    private static final String DESTINATION_PHONE = "+380663127876";
    private static final String DESTINATION_NAME = "vodafone";
    private static final String DESTINATION_EMAIL = "test@gmail.com";
    private static final Long OTP_ID = 1L;
    private static final String OTP = "otp";
    private static final String LANG_KEY = "en";
    private static final String GENERATE_OTP_URL = "http://otp-app/api/one-time-password";
    private static final String CHECK_OTP_URL = "http://otp-app/api/one-time-password/check";

    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private TenantProperties.Security security;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private OtpServiceClient otpServiceClient;
    @InjectMocks
    private OtpService otpService;

    @Before
    public void setup() throws Exception {
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf(TENANT)));

        TenantContextHolder tenantContextHolder = mock(TenantContextHolder.class);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
        when(tenantProperties.getSecurity()).thenReturn(security);

        when(tenantProperties.getSecurity().getTfaOtpTypeKey()).thenReturn(TFA_OTP_TYPE_KEY);
    }

    @Test
    public void prepareOtpRequest_phoneNumber() {
        when(tenantProperties.getSecurity().getTfaOtpGenerateUrl()).thenReturn(GENERATE_OTP_URL);
        when(tenantProperties.getSecurity().getTfaOtpReceiverTypeKey()).thenReturn(PHONE_NUMBER.getValue());
        OneTimePasswordDto expectedBody = buildOneTimePasswordDto(DESTINATION_PHONE, PHONE_NUMBER.getValue());

        otpService.prepareOtpRequest(buildDomainUserDetails(UserLoginType.MSISDN.getValue(), DESTINATION_PHONE));

        verify(otpServiceClient).createOtp(ArgumentMatchers.eq(GENERATE_OTP_URL), ArgumentMatchers.eq(expectedBody));
    }

    @Test
    public void prepareOtpRequest_nickname_withCustomTfaOtpConfigAndReceiverType() {
        when(tenantProperties.getSecurity().getTfaOtpGenerateUrl()).thenReturn(GENERATE_OTP_URL);
        when(tenantProperties.getSecurity().getTfaOtpReceiverTypeKey()).thenReturn(PHONE_NUMBER.getValue());
        var userDetails = buildDomainUserDetailsCustom(UserLoginType.NICKNAME.getValue(), DESTINATION_NAME);

        otpService.prepareOtpRequest(userDetails);

        OneTimePasswordDto expectedBody = buildCustomOneTimePasswordDto(DESTINATION_NAME, NAME.getValue());

        verify(otpServiceClient).createOtp(ArgumentMatchers.eq(GENERATE_OTP_URL), ArgumentMatchers.eq(expectedBody));
    }

    @Test
    public void prepareOtpRequest_email() {
        when(tenantProperties.getSecurity().getTfaOtpGenerateUrl()).thenReturn(GENERATE_OTP_URL);
        when(tenantProperties.getSecurity().getTfaOtpReceiverTypeKey()).thenReturn(EMAIL.getValue());
        OneTimePasswordDto expectedBody = buildOneTimePasswordDto(DESTINATION_EMAIL, EMAIL.getValue());

        otpService.prepareOtpRequest(buildDomainUserDetails(UserLoginType.EMAIL.getValue(), DESTINATION_EMAIL));

        verify(otpServiceClient).createOtp(ArgumentMatchers.eq(GENERATE_OTP_URL), ArgumentMatchers.eq(expectedBody));
    }

    @Test(expected = BusinessException.class)
    public void prepareOtpRequest_generateOtpUrlNotExist() {
        when(tenantProperties.getSecurity().getTfaOtpGenerateUrl()).thenReturn(null);

        otpService.prepareOtpRequest(buildDomainUserDetails("", ""));
    }

    @Test(expected = BusinessException.class)
    public void prepareOtpRequest_UserLoginTypeNotFound() {
        when(tenantProperties.getSecurity().getTfaOtpGenerateUrl()).thenReturn(GENERATE_OTP_URL);
        when(tenantProperties.getSecurity().getTfaOtpReceiverTypeKey()).thenReturn(PHONE_NUMBER.getValue());

        otpService.prepareOtpRequest(buildDomainUserDetails("", ""));
    }

    @Test(expected = NotImplementedException.class)
    public void prepareOtpRequest_unknownReceiverTypeKey() {
        when(tenantProperties.getSecurity().getTfaOtpGenerateUrl()).thenReturn(GENERATE_OTP_URL);
        when(tenantProperties.getSecurity().getTfaOtpReceiverTypeKey()).thenReturn(null);

        otpService.prepareOtpRequest(buildDomainUserDetails("", ""));
    }

    @Test
    public void checkOtpRequest() {
        when(tenantProperties.getSecurity().getTfaOtpCheckUrl()).thenReturn(CHECK_OTP_URL);
        OneTimePasswordCheckDto expectedBody = buildOneTimePasswordCheckDto();

        otpService.checkOtpRequest(OTP_ID, OTP);

        verify(otpServiceClient).checkOtp(ArgumentMatchers.eq(CHECK_OTP_URL), ArgumentMatchers.eq(expectedBody));
    }

    @Test(expected = BusinessException.class)
    public void checkOtpRequest_checkOtpUrlNotExist() {
        when(tenantProperties.getSecurity().getTfaOtpCheckUrl()).thenReturn(null);

        otpService.checkOtpRequest(null, null);
    }

    private DomainUserDetails buildDomainUserDetails(String userLoginType, String login) {
        return new DomainUserDetails(
            "aroze",
            "password",
            emptyList(),
            "",
            "testUserKey",
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
            singletonList(buildUserLoginDto(buildUserLogin(userLoginType, login))),
            LANG_KEY
        );
    }


    private DomainUserDetails buildDomainUserDetailsCustom(String userLoginType, String login) {
        DomainUserDetails details = new DomainUserDetails(
            "username",
            "password",
            emptyList(),
            "",
            "testUserKey",
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
            singletonList(buildUserLoginDto(buildUserLogin(userLoginType, login))),
            LANG_KEY
        );

        details.getAdditionalDetails().put(TOKEN_AUTH_DETAILS_TFA_OTP_TYPE_KEY, TFA_OTP_TYPE_KEY_CUSTOM);
        details.getAdditionalDetails().put(TOKEN_AUTH_DETAILS_TFA_OTP_RECEIVER_TYPE_KEY, NAME.getValue());
        return details;
    }

    private UserLogin buildUserLogin(String typeKey, String login) {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(typeKey);
        userLogin.setLogin(login);

        return userLogin;
    }

    private UserLoginDto buildUserLoginDto(UserLogin userLogin) {
        return new UserLoginDto(userLogin);
    }

    private OneTimePasswordDto buildOneTimePasswordDto(String destination, String receiverTypeKey) {
        return new OneTimePasswordDto(null, destination, receiverTypeKey, TFA_OTP_TYPE_KEY, LANG_KEY);
    }

    private OneTimePasswordDto buildCustomOneTimePasswordDto(String destination, String receiverTypeKey) {
        return new OneTimePasswordDto(null, destination, receiverTypeKey, TFA_OTP_TYPE_KEY_CUSTOM, LANG_KEY);
    }

    private OneTimePasswordCheckDto buildOneTimePasswordCheckDto() {
        return new OneTimePasswordCheckDto(OTP_ID, OTP);
    }

}
