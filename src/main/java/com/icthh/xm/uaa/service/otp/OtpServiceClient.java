package com.icthh.xm.uaa.service.otp;

import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceClient {

    private static final String GRANT_TYPE = "grant_type";
    private static final String GRANT_TYPE_VALUE = "client_credentials";
    private static final String X_TENANT = "x-tenant";
    private static final String BEARER = "Bearer ";

    @Qualifier("loadBalancedRestTemplate")
    private final RestTemplate restTemplate;

    public String getSystemToken(String url, String systemClientToken, String tenant) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(X_TENANT, tenant);
        headers.add(AUTHORIZATION, systemClientToken);

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(GRANT_TYPE, GRANT_TYPE_VALUE);

        try {
            return (String) restTemplate.exchange(url, POST, new HttpEntity<>(body, headers), Map.class).getBody().get("access_token");
        } catch (Exception e) {
            log.error("Can't get system token", e);
            throw new BusinessException("Can't get system token");
        }
    }

    public Long createOtp(String url, OtpService.OneTimePasswordDto body, String systemToken) {
        log.info("OneTimePasswordCustom body: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(AUTHORIZATION, BEARER + systemToken);

        ResponseEntity<OtpService.OneTimePasswordDto> response;
        try {
            response = restTemplate.exchange(url, POST, new HttpEntity<>(body, headers), OtpService.OneTimePasswordDto.class);
        } catch (Exception e) {
            log.error("Can't get otp id", e);
            throw new BusinessException("Can't get otp id");
        }
        return response.getBody().getId();
    }

    public boolean checkOtp(String url, OtpService.OneTimePasswordCheckDto body, String systemToken) {
        log.info("OneTimePasswordCheck body: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(AUTHORIZATION, BEARER + systemToken);

        try {
            restTemplate.exchange(url, POST, new HttpEntity<>(body, headers), OtpService.OneTimePasswordDto.class);
        } catch (Exception e) {
            log.error("Can't check otp", e);
            return false;
        }

        return true;
    }

}
