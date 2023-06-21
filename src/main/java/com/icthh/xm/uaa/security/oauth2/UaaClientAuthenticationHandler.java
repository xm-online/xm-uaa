package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.auth.DefaultClientAuthenticationHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;

@Component
@RequiredArgsConstructor
public class UaaClientAuthenticationHandler extends DefaultClientAuthenticationHandler {

    private final TenantContextHolder tenantContextHolder;

    @Override
    public void authenticateTokenRequest(OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form, HttpHeaders headers) {
        headers.add(HEADER_TENANT, tenantContextHolder.getTenantKey());
        super.authenticateTokenRequest(resource, form, headers);
    }
}
