package com.icthh.xm.uaa.security;

import static com.icthh.xm.uaa.config.Constants.AUTH_ADDITIONAL_DETAILS;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import java.util.HashMap;
import java.util.Map;

import com.icthh.xm.uaa.lep.keyresolver.OptionalProfileHeaderResolver;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Component
@LepService(group = "security")
public class DomainJwtAccessTokenDetailsPostProcessor {

    @LogicExtensionPoint(value = "ProcessJwtAccessTokenDetails", resolver = OptionalProfileHeaderResolver.class)
    public void processJwtAccessTokenDetails(OAuth2Authentication authentication, Map<String, Object> details) {
        Map<String, Object> additionalDetails = new HashMap<>(authentication.getOAuth2Request().getRequestParameters());
        // password and client_secret removed by spring
        additionalDetails.remove("grant_type");
        additionalDetails.remove("username");
        additionalDetails.remove("token");
        if (!additionalDetails.isEmpty()) {
            details.put(AUTH_ADDITIONAL_DETAILS, additionalDetails);
        }
    }

}
