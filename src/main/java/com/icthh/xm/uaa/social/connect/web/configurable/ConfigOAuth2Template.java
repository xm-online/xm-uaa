package com.icthh.xm.uaa.social.connect.web.configurable;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.util.MultiValueMap;

public class ConfigOAuth2Template extends OAuth2Template {
    public ConfigOAuth2Template(Social social) {
        super(social.getClientId(), social.getClientSecret(), social.getAuthorizeUrl(), social.getAccessTokenUrl());
        setUseParametersForClientAuthentication(true);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected AccessGrant postForAccessGrant(String accessTokenUrl, MultiValueMap<String, String> parameters) {
        // TODO use parent method instead
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(parameters, headers);
        ResponseEntity<Map> responseEntity = getRestTemplate().exchange(accessTokenUrl, HttpMethod.POST, requestEntity, Map.class);
        Map<String, Object> responseMap = responseEntity.getBody();
        return extractAccessGrant(responseMap);
    }

    private AccessGrant extractAccessGrant(Map<String, Object> result) {
        String accessToken = (String) result.get("access_token");
        String scope = (String) result.get("scope");
        String refreshToken = (String) result.get("refresh_token");
        Number expiresInNumber = (Number) result.get("expires_in");
        Long expiresIn = (expiresInNumber == null) ? null : expiresInNumber.longValue();
        return createAccessGrant(accessToken, scope, refreshToken, expiresIn, result);
    }

}
