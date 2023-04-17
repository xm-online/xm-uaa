package com.icthh.xm.uaa.service.otp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.UserLoginDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String UAA_CONFIG_KEY = "uaa";
    private static final String OTP_CONFIG_KEY = "otp";
    private static final String GENERATE_OTP_CONFIG_KEY = "url";
    private static final String CHECK_OTP_CONFIG_KEY = "check";

    private final TenantPropertiesService tenantPropertiesService;
    private final TenantContextHolder tenantContextHolder;
    private final TenantConfigService tenantConfigService;
    private final OtpServiceClient otpServiceClient;
    private final ObjectMapper mapper;

    public String getSystemTokenRequest() {
        Object uaaConfig = tenantConfigService.getConfig().get(UAA_CONFIG_KEY);

        UaaConfigDto uaaConfigDto = mapper.convertValue(uaaConfig, UaaConfigDto.class);

        String systemAuthUrl = uaaConfigDto.getSystemAuthUrl();
        String systemClientToken = uaaConfigDto.getSystemClientToken();
        String tenant = tenantContextHolder.getTenantKey();
        if (StringUtils.isEmpty(systemAuthUrl) || StringUtils.isEmpty(systemClientToken) || StringUtils.isEmpty(tenant)) {
            log.error("systemAuthUrl: {}, systemClientToken: {}, tenant: {}", systemAuthUrl, systemClientToken, tenant);
            throw new BusinessException("error.create.otp.system.token", "Can not create otp system token");
        }

        return otpServiceClient.getSystemToken(systemAuthUrl, systemClientToken, tenant);
    }

    public Long getOtpRequest(DomainUserDetails userDetails) {
        String systemToken = getSystemTokenRequest();

        String url = buildOneTimePasswordUrl(GENERATE_OTP_CONFIG_KEY);
        if (StringUtils.isEmpty(url)) {
            log.error("OneTimePasswordUrl is empty: {}", url);
            throw new BusinessException("error.get.otp.password.url", "Can not get otp password url");
        }

        String tfaOtpTypeKey = tenantPropertiesService.getTenantProps().getSecurity().getTfaOtpTypeKey();
        log.info("tfaOtpTypeKey: {}", tfaOtpTypeKey);
        String receiverTypeKey = tenantPropertiesService.getTenantProps().getSecurity().getTfaOtpReceiverTypeKey();
        log.info("receiverTypeKey: {}", receiverTypeKey);

        UserLoginDto userLogin = userDetails.getLogins().stream()
            .filter(UserLoginDto -> UserLoginType.MSISDN.getValue().equals(UserLoginDto.getTypeKey()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("error.find.otp.receiver", "Can not get otp receiver"));

        OneTimePasswordCustomDto oneTimePasswordCustomDto = new OneTimePasswordCustomDto();
        oneTimePasswordCustomDto.setReceiver(userLogin.getLogin());
        oneTimePasswordCustomDto.setReceiverTypeKey(receiverTypeKey);
        oneTimePasswordCustomDto.setTypeKey(tfaOtpTypeKey);
        oneTimePasswordCustomDto.setLangKey("uk");

        return otpServiceClient.getOtpRequest(url, oneTimePasswordCustomDto, systemToken);
    }

    public boolean checkOtpRequest(Long otpId, String otp) {
        String systemToken = getSystemTokenRequest();

        String url = buildOneTimePasswordUrl(CHECK_OTP_CONFIG_KEY);
        if (StringUtils.isEmpty(url)) {
            log.error("OneTimePasswordCheckUrl is empty: {}", url);
            throw new BusinessException("error.get.otp.password.check.url", "Can not get otp password check url");
        }

        OneTimePasswordCheckDto oneTimePasswordCheckDto = new OneTimePasswordCheckDto(otpId, otp);

        return otpServiceClient.checkOtpRequest(url, oneTimePasswordCheckDto, systemToken);
    }

    private String buildOneTimePasswordUrl(String fieldKey) {
        Object otpConfig = tenantConfigService.getConfig().get(OTP_CONFIG_KEY);

        OtpConfigDto otpConfigDto = mapper.convertValue(otpConfig, OtpConfigDto.class);

        String baseUrl = otpConfigDto.getBaseUrl();
        String oneTimePasswordUrl = otpConfigDto.getOneTimePassword().get(fieldKey);

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(oneTimePasswordUrl)) {
            log.error("baseUrl: {}, oneTimePasswordUrl: {}", baseUrl, oneTimePasswordUrl);
            throw new BusinessException("error.build.otp.url", "Can not build otp url");
        }

        return baseUrl + oneTimePasswordUrl;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OneTimePasswordCustomDto implements Serializable {
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
