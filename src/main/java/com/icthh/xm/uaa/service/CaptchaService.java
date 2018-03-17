package com.icthh.xm.uaa.service;

import static org.apache.commons.lang.StringUtils.isBlank;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.RegistrationLog;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
@LepService(group = "service.captcha")
public class CaptchaService {
    private static final int DEFAULT_CAPTCHA_PERIOD = 3600;
    private final ApplicationProperties applicationProperties;
    private final RegistrationLogRepository registrationLogRepository;
    private final TenantPropertiesService tenantPropertiesService;

    public boolean checkCaptcha(String captcha) {
        if (isBlank(captcha)) {
            throw new ErrorCheckCaptcha("error.captcha.required", "Captcha is blank");
        }

        String url = getReCaptcha().getUrl() + "?secret=" + getReCaptcha().getSecretKey() + "&response=" + captcha;
        log.debug("Check captcha by url {}", url);

        final ObjectReader reader = new ObjectMapper().readerFor(Map.class);
        try {
            final MappingIterator<Map<String, Object>> result = reader.readValues(new URL(url));
            Map<String, Object> resultMap = result.nextValue();
            log.info("Captacha result map {}", resultMap);
            Boolean success = (Boolean) resultMap.get("success");
            return success;
        } catch (IOException e) {
            throw new ErrorCheckCaptcha(e);
        }
    }

    @LogicExtensionPoint("IsCaptchaNeed")
    public boolean isCaptchaNeed(String ipAddress) {
        long captchaPeriod = getRegistrationCaptchaPeriodSeconds();
        Optional<RegistrationLog> registration = registrationLogRepository.findOneByIpAddress(ipAddress);
        return registration.map(rl -> rl.moreThenSecondsAgo(captchaPeriod)).orElse(false);
    }

    private long getRegistrationCaptchaPeriodSeconds() {
        Long tenantValue = tenantPropertiesService.getTenantProps().getRegistrationCaptchaPeriodSeconds();
        Long appValue = applicationProperties.getReCaptcha().getRegistrationCaptchaPeriodSeconds();
        Long resultValue = tenantValue != null ? tenantValue : appValue;
        return resultValue != null ? resultValue : DEFAULT_CAPTCHA_PERIOD;
    }

    public String getPublicKey() {
        return getReCaptcha().getPublicKey();
    }

    private ApplicationProperties.ReCaptcha getReCaptcha() {
        return applicationProperties.getReCaptcha();
    }

    public static class ErrorCheckCaptcha extends BusinessException {

        private static final long serialVersionUID = 1L;

        public ErrorCheckCaptcha(String code, String message) {
            super(code, message);
        }

        public ErrorCheckCaptcha(Throwable cause) {
            super("error.captcha.check", cause.getMessage());
        }

    }

}
