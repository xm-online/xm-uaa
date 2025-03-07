package com.icthh.xm.uaa.service.otp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.UserLoginDto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_DESTINATION;
import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_OTP_GENERATE_URL;
import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_OTP_RECEIVER_TYPE_KEY;
import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_OTP_TYPE_KEY;

@Slf4j
@Service
@LepService(group = "service.otp")
@RequiredArgsConstructor
public class OtpService {

    private final TenantPropertiesService tenantPropertiesService;

    private final OtpServiceClient otpServiceClient;

    @LoggingAspectConfig(inputExcludeParams = "userDetails")
    @LogicExtensionPoint("prepareOtpRequest")
    public Long prepareOtpRequest(@NonNull final DomainUserDetails userDetails) {
        log.debug("Prepare otp request for user: {}", userDetails);
        Map<String, String> additionalDetails = userDetails.getAdditionalDetails();
        log.debug("Additional details: {}", additionalDetails);
        String url = Optional.ofNullable(additionalDetails.get(TOKEN_AUTH_DETAILS_TFA_OTP_GENERATE_URL))
            .orElse(tenantPropertiesService.getTenantProps().getSecurity().getTfaOtpGenerateUrl());
        if (StringUtils.isEmpty(url)) {
            log.error("OneTimePasswordUrl is empty: {}", url);
            throw new BusinessException("error.get.otp.password.url", "Can not get otp password url");
        }
        log.debug("Otp url: {}", url);
        String tfaOtpTypeKey = Optional.ofNullable(additionalDetails.get(TOKEN_AUTH_DETAILS_TFA_OTP_TYPE_KEY))
            .orElse(tenantPropertiesService.getTenantProps().getSecurity().getTfaOtpTypeKey());
        log.info("tfaOtpTypeKey: {}", tfaOtpTypeKey);
        String receiverTypeKey = Optional.ofNullable(additionalDetails.get(TOKEN_AUTH_DETAILS_TFA_OTP_RECEIVER_TYPE_KEY))
            .orElse(tenantPropertiesService.getTenantProps().getSecurity().getTfaOtpReceiverTypeKey());
        log.info("receiverTypeKey: {}", receiverTypeKey);

        UserLoginDto userLogin = findUserLogin(userDetails, receiverTypeKey);

        String destination = userLogin.getLogin();
        log.debug("destination: {}", destination);
        additionalDetails.put(TOKEN_AUTH_DETAILS_TFA_DESTINATION, destination);

        OneTimePasswordDto oneTimePasswordDto = new OneTimePasswordDto();
        oneTimePasswordDto.setReceiver(destination);
        oneTimePasswordDto.setReceiverTypeKey(receiverTypeKey);
        oneTimePasswordDto.setTypeKey(tfaOtpTypeKey);
        oneTimePasswordDto.setLangKey(userDetails.getLangKey());

        return otpServiceClient.createOtp(url, oneTimePasswordDto);
    }

    @LoggingAspectConfig(inputExcludeParams = "otp")
    @LogicExtensionPoint("checkOtpRequest")
    public boolean checkOtpRequest(Long otpId, String otp) {

        String url = tenantPropertiesService.getTenantProps().getSecurity().getTfaOtpCheckUrl();
        if (StringUtils.isEmpty(url)) {
            log.error("OneTimePasswordCheckUrl is empty: {}", url);
            throw new BusinessException("error.get.otp.password.check.url", "Can not get otp password check url");
        }

        OneTimePasswordCheckDto oneTimePasswordCheckDto = new OneTimePasswordCheckDto(otpId, otp);

        return otpServiceClient.checkOtp(url, oneTimePasswordCheckDto);
    }

    private UserLoginDto findUserLogin(final DomainUserDetails userDetails, final String receiverTypeKey) {
        log.info("Find user login by receiverTypeKey: {}", receiverTypeKey);
        UserLoginDto userLogin;
        if (ReceiverTypeKey.PHONE_NUMBER.getValue().equalsIgnoreCase(receiverTypeKey)) {
            userLogin = userDetails.getLogins().stream()
                .filter(UserLoginDto -> UserLoginType.MSISDN.getValue().equalsIgnoreCase(UserLoginDto.getTypeKey()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("error.find.otp.phone.number.receiver", "Can not get otp phone number receiver"));
        } else if (ReceiverTypeKey.EMAIL.getValue().equalsIgnoreCase(receiverTypeKey)) {
            userLogin = userDetails.getLogins().stream()
                .filter(UserLoginDto -> UserLoginType.EMAIL.getValue().equalsIgnoreCase(UserLoginDto.getTypeKey()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("error.find.otp.email.receiver", "Can not get otp email receiver"));
        } else if (ReceiverTypeKey.NAME.getValue().equalsIgnoreCase(receiverTypeKey)) {
            userLogin = userDetails.getLogins().stream()
                .filter(UserLoginDto -> UserLoginType.NICKNAME.getValue().equalsIgnoreCase(UserLoginDto.getTypeKey()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("error.find.otp.nickname.receiver", "Can not get otp name receiver"));
        } else {
            throw new NotImplementedException("Not implemented otp receiver type key: " + receiverTypeKey);
        }
        log.debug("userLogin: {}", userLogin);
        return userLogin;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class OneTimePasswordDto implements Serializable {
        private Long id;
        private String receiver;
        private String receiverTypeKey;
        private String typeKey;
        private String langKey;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class OneTimePasswordCheckDto implements Serializable {
        private Long id;
        private String otp;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UaaConfigDto implements Serializable {
        private String systemAuthUrl;
        private String systemClientToken;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OtpConfigDto implements Serializable {
        private String baseUrl;
        private Map<String, String> oneTimePassword = new HashMap<>();
    }
}
