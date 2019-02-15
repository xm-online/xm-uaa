package com.icthh.xm.uaa.social;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import org.springframework.social.oauth2.AbstractOAuth2ServiceProvider;
import org.springframework.social.oauth2.OAuth2Template;

public class ConfigServiceProvider extends AbstractOAuth2ServiceProvider<ConfigOAuth2Api> {

    private final Social social;

    public ConfigServiceProvider(Social social) {
        super(createOAuth2Template(social));
        this.social = social;
    }

    private static OAuth2Template createOAuth2Template(Social social) {
        OAuth2Template oAuth2Template = new OAuth2Template(social.getClientId(), social.getClientSecret(),
                                                            social.getAuthorizeUrl(), social.getAccessTokenUrl());
        oAuth2Template.setUseParametersForClientAuthentication(true);
        return oAuth2Template;
    }

    @Override
    public ConfigOAuth2Api getApi(String accessToken) {
        return new ConfigOAuth2Api(accessToken, social);
    }
}
