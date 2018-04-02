package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String DEFAULT_TENANT = "XM";

    private final TokenEndpoint tokenEndpoint;

    private final TenantContextHolder tenantContextHolder;

    public void authenticate() throws HttpRequestMethodNotSupportedException {
        log.info("Authenticating request for privileges config");
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT);
        OAuth2Request request = new OAuth2Request(Collections.singletonMap("grant_type", "client_credentials"),
            "internal", null, true, null, null, null, null, null);
        OAuth2Authentication authentication = new OAuth2Authentication(request, null);
        ResponseEntity<OAuth2AccessToken> tokenResponse = tokenEndpoint.postAccessToken(authentication,
            Collections.singletonMap("grant_type", "client_credentials"));
        authentication.setDetails(tokenResponse.getBody().getValue());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
