package com.icthh.xm.uaa.social;

import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.web.client.RestTemplate;

public class ConfigOAuth2Template extends OAuth2Template {

    public ConfigOAuth2Template(String clientId, String clientSecret, String authorizeUrl, String accessTokenUrl) {
        super(clientId, clientSecret, authorizeUrl, accessTokenUrl);
    }

    @Override
    public RestTemplate getRestTemplate() {
        return super.getRestTemplate();
    }
}
