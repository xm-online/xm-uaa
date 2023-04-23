package com.icthh.xm.uaa.service.otp;

import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;

import static org.springframework.http.HttpMethod.POST;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceClient {

    private final OAuth2RestTemplate oAuth2RestTemplate;

    public Long createOtp(String url, OtpService.OneTimePasswordDto body) {
        log.info("OneTimePasswordCustom body: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<OtpService.OneTimePasswordDto> response;
        try {
            response = oAuth2RestTemplate.exchange(url, POST, new HttpEntity<>(body, headers), OtpService.OneTimePasswordDto.class);
        } catch (Exception e) {
            log.error("Can't get otp id", e);
            throw new BusinessException("Can't get otp id");
        }
        return response.getBody().getId();
    }

    public boolean checkOtp(String url, OtpService.OneTimePasswordCheckDto body) {
        log.info("OneTimePasswordCheck body: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            oAuth2RestTemplate.exchange(url, POST, new HttpEntity<>(body, headers), OtpService.OneTimePasswordDto.class);
        } catch (Exception e) {
            log.error("Can't check otp", e);
            return false;
        }

        return true;
    }

}
