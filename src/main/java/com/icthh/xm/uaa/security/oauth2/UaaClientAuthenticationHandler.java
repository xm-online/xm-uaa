package com.icthh.xm.uaa.security.oauth2;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.auth.DefaultClientAuthenticationHandler;
import org.springframework.util.MultiValueMap;

import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static com.icthh.xm.uaa.config.Constants.SUPER_TENANT;

public class UaaClientAuthenticationHandler extends DefaultClientAuthenticationHandler {

    @Override
    public void authenticateTokenRequest(OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form, HttpHeaders headers) {
        headers.add(HEADER_TENANT, SUPER_TENANT);
        super.authenticateTokenRequest(resource, form, headers);
    }
}
